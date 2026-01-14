package com.collab.message.websocket;

import com.alibaba.fastjson2.JSON;
import com.collab.message.dto.MessageDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息WebSocket处理器
 * 使用Redis Pub/Sub实现跨实例消息推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {
    
    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    
    // 本实例的用户会话映射
    private static final Map<Long, WebSocketSession> localUserSessions = new ConcurrentHashMap<>();
    
    // Redis频道名称
    private static final String MESSAGE_CHANNEL = "message:push:channel";
    // 用户在线状态Key前缀
    private static final String USER_ONLINE_PREFIX = "message:online:";
    // 在线状态过期时间（秒）
    private static final long ONLINE_TTL = 300;
    
    // 同步锁，防止WebSocket并发写
    private static final Map<Long, Object> sessionLocks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 订阅Redis消息频道
        listenerContainer.addMessageListener((message, pattern) -> {
            try {
                String body = new String(message.getBody());
                handleRedisPushMessage(body);
            } catch (Exception e) {
                log.error("处理Redis推送消息失败", e);
            }
        }, new ChannelTopic(MESSAGE_CHANNEL));
        log.info("已订阅Redis消息推送频道: {}", MESSAGE_CHANNEL);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            localUserSessions.put(userId, session);
            sessionLocks.put(userId, new Object());
            // 在Redis中标记用户在线
            markUserOnline(userId);
            log.info("用户 {} 已连接消息WebSocket", userId);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Long userId = getUserIdFromSession(session);
        if ("ping".equals(payload)) {
            sendToLocalSession(userId, session, "pong");
            // 刷新在线状态
            if (userId != null) {
                markUserOnline(userId);
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            localUserSessions.remove(userId);
            sessionLocks.remove(userId);
            // 从Redis移除在线标记
            markUserOffline(userId);
            log.info("用户 {} 已断开消息WebSocket连接", userId);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            localUserSessions.remove(userId);
            sessionLocks.remove(userId);
            markUserOffline(userId);
        }
        if (session.isOpen()) {
            session.close();
        }
    }
    
    /**
     * 向指定用户发送消息（支持跨实例）
     */
    public void sendMessageToUser(Long userId, MessageDTO message) {
        String json = JSON.toJSONString(Map.of(
            "type", "NEW_MESSAGE",
            "data", message
        ));
        
        // 先尝试本地发送
        if (sendToLocalUser(userId, json)) {
            log.info("已向本地用户 {} 推送新消息", userId);
            return;
        }
        
        // 本地没有，发布到Redis让其他实例处理
        if (isUserOnline(userId)) {
            publishToRedis(userId, json);
            log.info("已通过Redis向用户 {} 推送新消息", userId);
        }
    }
    
    /**
     * 向指定用户发送未读数更新（支持跨实例）
     */
    public void sendUnreadCountUpdate(Long userId, int unreadCount) {
        String json = JSON.toJSONString(Map.of(
            "type", "UNREAD_COUNT",
            "data", unreadCount
        ));
        
        if (sendToLocalUser(userId, json)) {
            return;
        }
        
        if (isUserOnline(userId)) {
            publishToRedis(userId, json);
        }
    }
    
    /**
     * 检查用户是否在线（通过Redis）
     */
    public boolean isUserOnline(Long userId) {
        // 先检查本地
        WebSocketSession session = localUserSessions.get(userId);
        if (session != null && session.isOpen()) {
            return true;
        }
        // 检查Redis
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_ONLINE_PREFIX + userId));
    }
    
    // ========== 私有方法 ==========
    
    private void markUserOnline(Long userId) {
        redisTemplate.opsForValue().set(
            USER_ONLINE_PREFIX + userId, 
            "1", 
            java.time.Duration.ofSeconds(ONLINE_TTL)
        );
    }
    
    private void markUserOffline(Long userId) {
        redisTemplate.delete(USER_ONLINE_PREFIX + userId);
    }
    
    /**
     * 发送消息到本地用户会话（带同步锁防止并发写）
     */
    private boolean sendToLocalUser(Long userId, String json) {
        WebSocketSession session = localUserSessions.get(userId);
        if (session != null && session.isOpen()) {
            return sendToLocalSession(userId, session, json);
        }
        return false;
    }
    
    /**
     * 同步发送消息到WebSocket会话
     */
    private boolean sendToLocalSession(Long userId, WebSocketSession session, String json) {
        Object lock = sessionLocks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    return true;
                } catch (IOException e) {
                    log.error("向用户 {} 发送消息失败", userId, e);
                }
            }
        }
        return false;
    }
    
    /**
     * 发布消息到Redis频道
     */
    private void publishToRedis(Long userId, String json) {
        String redisMessage = JSON.toJSONString(Map.of(
            "userId", userId,
            "payload", json
        ));
        redisTemplate.convertAndSend(MESSAGE_CHANNEL, redisMessage);
    }
    
    /**
     * 处理从Redis接收到的推送消息
     */
    private void handleRedisPushMessage(String message) {
        try {
            Map<String, Object> data = JSON.parseObject(message);
            Long userId = Long.valueOf(data.get("userId").toString());
            String payload = data.get("payload").toString();
            
            // 检查本地是否有该用户的会话
            sendToLocalUser(userId, payload);
        } catch (Exception e) {
            log.error("解析Redis推送消息失败: {}", message, e);
        }
    }
    
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            return Long.valueOf(userId.toString());
        }
        return null;
    }
}
