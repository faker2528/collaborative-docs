package com.collab.collaboration.handler;

import com.alibaba.fastjson2.JSON;
import com.collab.collaboration.client.DocumentClient;
import com.collab.collaboration.model.CollaborationRoom;
import com.collab.collaboration.model.CollaborationUser;
import com.collab.collaboration.model.WebSocketMessage;
import com.collab.collaboration.model.WebSocketMessage.MessageType;
import com.collab.collaboration.model.WebSocketMessage.OnlineUser;
import com.collab.collaboration.service.CollaborationRoomManager;
import com.collab.common.crdt.CrdtOperation;
import com.collab.common.dto.DocumentDTO;
import com.collab.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 协作WebSocket处理器
 * 处理所有WebSocket消息，实现实时协作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private final CollaborationRoomManager roomManager;
    private final DocumentClient documentClient;

    /**
     * 所有活跃会话（sessionId -> CollaborationUser）
     */
    private final Map<String, CollaborationUser> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Session 发送锁（避免并发发送冲突）
     */
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    
    /**
     * 获取 session 的锁对象
     */
    private Object getSessionLock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        Long documentId = (Long) session.getAttributes().get("documentId");

        log.info("WebSocket connection established: user={}, document={}, session={}", 
                username, documentId, session.getId());

        // 创建协作用户
        CollaborationUser user = new CollaborationUser(userId, username, session, documentId);
        activeSessions.put(session.getId(), user);

        // 检查房间是否已经存在（如果是第一个用户加入，需要初始化文档内容）
        CollaborationRoom room = roomManager.getRoom(documentId);
        if (room == null || room.isEmpty()) {
            // 需要从文档服务加载初始内容
            initRoomContent(documentId, userId);
            // 设置房间创建者
            room = roomManager.getRoom(documentId);
            if (room != null) {
                room.setCreatorUserId(userId);
            }
        }

        // 加入协作房间
        roomManager.joinRoom(documentId, user);

        // 发送加入成功消息（包含当前文档内容和在线用户）
        sendJoinedMessage(session, user);

        // 广播用户加入消息给其他用户
        broadcastUserJoined(documentId, user, session.getId());
    }
    
    /**
     * 初始化房间文档内容
     */
    private void initRoomContent(Long documentId, Long userId) {
        int maxRetries = 3;
        int retryDelay = 500; // 毫秒
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Result<DocumentDTO> result = documentClient.getDocument(documentId, userId);
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    String content = result.getData().getContent();
                    if (content != null && !content.isEmpty()) {
                        roomManager.initDocumentContent(documentId, content);
                        log.info("Initialized room {} with document content", documentId);
                    }
                    return; // 成功，退出
                }
            } catch (Exception e) {
                log.warn("Failed to load document content for room {} (attempt {}/{}): {}", 
                        documentId, i + 1, maxRetries, e.getMessage());
                
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("Failed to load document content for room {} after {} retries, starting with empty content", 
                documentId, maxRetries);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        CollaborationUser user = activeSessions.get(session.getId());
        if (user == null) {
            log.warn("Received message from unknown session: {}", session.getId());
            return;
        }

        try {
            WebSocketMessage wsMessage = JSON.parseObject(message.getPayload(), WebSocketMessage.class);
            wsMessage.setUserId(user.getUserId());
            wsMessage.setUsername(user.getUsername());
            wsMessage.setSiteId(user.getSiteId());
            wsMessage.setTimestamp(System.currentTimeMillis());

            log.debug("Received message: type={}, user={}, document={}", 
                    wsMessage.getType(), user.getUsername(), user.getDocumentId());

            handleMessage(session, user, wsMessage);

        } catch (Exception e) {
            log.error("Error handling message from user {}: {}", user.getUsername(), e.getMessage(), e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理不同类型的消息
     */
    private void handleMessage(WebSocketSession session, CollaborationUser user, WebSocketMessage message) {
        switch (message.getType()) {
            case OPERATION:
                handleOperation(user, message.getOperation());
                break;
            case OPERATIONS:
                handleOperations(user, message.getOperations());
                break;
            case SYNC_REQUEST:
                handleSyncRequest(session, user);
                break;
            default:
                log.warn("Unknown message type: {}", message.getType());
        }
    }

    /**
     * 处理单个操作（Delta 格式）
     * 解析 Delta 并转换为 CRDT 字符级别操作
     */
    private void handleOperation(CollaborationUser user, CrdtOperation operation) {
        if (operation == null) {
            return;
        }

        Long documentId = user.getDocumentId();
        
        // 从 operation.character.value 中提取 Delta JSON
        if (operation.getCharacter() != null && operation.getCharacter().getValue() != null) {
            String deltaJson = operation.getCharacter().getValue();
            // 应用 Delta 到 CRDT 文档（转换为字符级别操作）
            roomManager.applyDelta(documentId, deltaJson, user.getSiteId());
        }

        // 广播原始 Delta 操作给其他用户（前端直接应用 Delta）
        broadcastOperation(documentId, operation, user.getSession().getId());
        
        log.debug("Processed delta operation via CRDT: user={}, document={}", 
                user.getUsername(), documentId);
    }

    /**
     * 处理批量操作
     */
    private void handleOperations(CollaborationUser user, List<CrdtOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return;
        }

        Long documentId = user.getDocumentId();

        // 应用所有操作
        for (CrdtOperation operation : operations) {
            if (operation.getCharacter() != null && operation.getCharacter().getValue() != null) {
                String deltaJson = operation.getCharacter().getValue();
                roomManager.applyDelta(documentId, deltaJson, user.getSiteId());
            }
        }

        // 广播操作给其他用户
        broadcastOperations(documentId, operations, user.getSession().getId());
        
        log.debug("Processed {} delta operations via CRDT from user={}, document={}", 
                operations.size(), user.getUsername(), documentId);
    }

    /**
     * 处理同步请求
     */
    private void handleSyncRequest(WebSocketSession session, CollaborationUser user) {
        String content = roomManager.getDocumentContent(user.getDocumentId());
        
        WebSocketMessage response = new WebSocketMessage();
        response.setType(MessageType.SYNC_RESPONSE);
        response.setDocumentId(user.getDocumentId());
        response.setContent(content);
        response.setTimestamp(System.currentTimeMillis());

        sendMessage(session, response);
        log.debug("Sent sync response to user={}, document={}", user.getUsername(), user.getDocumentId());
    }

    /**
     * 发送加入成功消息
     */
    private void sendJoinedMessage(WebSocketSession session, CollaborationUser user) {
        Long documentId = user.getDocumentId();
        
        WebSocketMessage response = new WebSocketMessage();
        response.setType(MessageType.JOINED);
        response.setDocumentId(documentId);
        response.setSiteId(user.getSiteId());
        response.setContent(roomManager.getDocumentContent(documentId));
        response.setOnlineUsers(getOnlineUsers(documentId));
        response.setTimestamp(System.currentTimeMillis());

        sendMessage(session, response);
    }

    /**
     * 广播用户加入消息
     */
    private void broadcastUserJoined(Long documentId, CollaborationUser newUser, String excludeSessionId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(MessageType.USER_JOINED);
        message.setDocumentId(documentId);
        message.setUserId(newUser.getUserId());
        message.setUsername(newUser.getUsername());
        message.setSiteId(newUser.getSiteId());
        message.setOnlineUsers(getOnlineUsers(documentId));
        message.setTimestamp(System.currentTimeMillis());

        broadcastToRoom(documentId, message, excludeSessionId);
    }

    /**
     * 广播用户离开消息
     */
    private void broadcastUserLeft(Long documentId, CollaborationUser user) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(MessageType.USER_LEFT);
        message.setDocumentId(documentId);
        message.setUserId(user.getUserId());
        message.setUsername(user.getUsername());
        message.setSiteId(user.getSiteId());
        message.setOnlineUsers(getOnlineUsers(documentId));
        message.setTimestamp(System.currentTimeMillis());

        broadcastToRoom(documentId, message, null);
    }

    /**
     * 广播单个操作
     */
    private void broadcastOperation(Long documentId, CrdtOperation operation, String excludeSessionId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(MessageType.REMOTE_OPERATION);
        message.setDocumentId(documentId);
        message.setOperation(operation);
        message.setTimestamp(System.currentTimeMillis());

        broadcastToRoom(documentId, message, excludeSessionId);
    }

    /**
     * 广播批量操作
     */
    private void broadcastOperations(Long documentId, List<CrdtOperation> operations, String excludeSessionId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(MessageType.REMOTE_OPERATIONS);
        message.setDocumentId(documentId);
        message.setOperations(operations);
        message.setTimestamp(System.currentTimeMillis());

        broadcastToRoom(documentId, message, excludeSessionId);
    }

    /**
     * 广播消息到房间内所有用户
     */
    private void broadcastToRoom(Long documentId, WebSocketMessage message, String excludeSessionId) {
        Collection<CollaborationUser> users = roomManager.getRoomUsers(documentId);
        String jsonMessage = JSON.toJSONString(message);

        for (CollaborationUser user : users) {
            WebSocketSession session = user.getSession();
            if (session.isOpen() && !session.getId().equals(excludeSessionId)) {
                sendMessageSafe(session, jsonMessage);
            }
        }
    }
    
    /**
     * 线程安全地发送消息
     */
    private void sendMessageSafe(WebSocketSession session, String jsonMessage) {
        synchronized (getSessionLock(session.getId())) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        String jsonMessage = JSON.toJSONString(message);
        sendMessageSafe(session, jsonMessage);
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String error) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(MessageType.ERROR);
        message.setError(error);
        message.setTimestamp(System.currentTimeMillis());
        sendMessage(session, message);
    }

    /**
     * 获取在线用户列表
     */
    private List<OnlineUser> getOnlineUsers(Long documentId) {
        return roomManager.getRoomUsers(documentId).stream()
                .map(u -> new OnlineUser(u.getUserId(), u.getUsername(), u.getSiteId()))
                .collect(Collectors.toList());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        CollaborationUser user = activeSessions.remove(session.getId());
        sessionLocks.remove(session.getId()); // 清理锁
        
        if (user != null) {
            Long documentId = user.getDocumentId();
            
            // 先从房间移除用户（这样 getOnlineUsers 获取的列表才是正确的）
            roomManager.leaveRoom(documentId, session.getId());
            
            // 再广播用户离开消息（此时在线列表已更新）
            broadcastUserLeft(documentId, user);
            
            // 检查房间是否为空，如果为空则保存文档内容
            CollaborationRoom room = roomManager.getRoom(documentId);
            if (room != null && room.isEmpty()) {
                saveDocumentContent(documentId, room);
            }
            
            log.info("WebSocket connection closed: user={}, document={}, status={}", 
                    user.getUsername(), documentId, status);
        }
    }
    
    /**
     * 保存文档内容到数据库
     */
    private void saveDocumentContent(Long documentId, CollaborationRoom room) {
        // 只在有未保存的编辑时才保存
        if (!room.isDirty()) {
            log.debug("Room {} has no unsaved changes, skip auto-save", documentId);
            return;
        }
        
        try {
            // 获取 Delta 格式的内容
            String deltaContent = room.getDeltaContent();
            if (deltaContent == null || deltaContent.isEmpty() || deltaContent.equals("{\"ops\":[]}")) {
                log.debug("Room {} content is empty, skip saving", documentId);
                return;
            }
            
            // 使用房间创建者的ID来保存
            Long creatorId = room.getCreatorUserId();
            if (creatorId == null) {
                log.warn("Room {} has no creator, cannot save", documentId);
                return;
            }
            
            Result<DocumentDTO> result = documentClient.updateDocument(documentId, deltaContent, creatorId);
            if (result != null && result.getCode() == 200) {
                room.clearDirty(); // 清除脏标记
                log.info("Auto-saved document {} content on room empty", documentId);
            } else {
                log.warn("Failed to auto-save document {}: {}", documentId, 
                        result != null ? result.getMessage() : "null response");
            }
        } catch (Exception e) {
            log.error("Failed to auto-save document {}: {}", documentId, e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        
        CollaborationUser user = activeSessions.get(session.getId());
        if (user != null) {
            log.error("Error for user: {}", user.getUsername());
        }
    }
}
