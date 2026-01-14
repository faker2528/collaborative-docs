package com.collab.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.common.dto.UserDTO;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.Result;
import com.collab.message.client.UserClient;
import com.collab.message.dto.ConversationDTO;
import com.collab.message.dto.MessageDTO;
import com.collab.message.dto.SendMessageRequest;
import com.collab.message.entity.Conversation;
import com.collab.message.entity.Message;
import com.collab.message.mapper.ConversationMapper;
import com.collab.message.mapper.MessageMapper;
import com.collab.message.service.MessageService;
import com.collab.message.websocket.MessageWebSocketHandler;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserClient userClient;
    private final MessageWebSocketHandler webSocketHandler;
    private final StringRedisTemplate redisTemplate;
    
    // Redis缓存key前缀
    private static final String USER_CACHE_PREFIX = "message:user:";
    private static final String UNREAD_COUNT_PREFIX = "message:unread:";
    // 缓存过期时间（分钟）
    private static final long USER_CACHE_TTL = 30;
    private static final long UNREAD_COUNT_TTL = 60;
    
    @Override
    public List<ConversationDTO> getConversationList(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .orderByDesc(Conversation::getPinned)
               .orderByDesc(Conversation::getLastMessageTime);
        
        List<Conversation> conversations = conversationMapper.selectList(wrapper);
        List<ConversationDTO> result = new ArrayList<>();
        
        for (Conversation conv : conversations) {
            ConversationDTO dto = convertToDTO(conv);
            // 填充对方用户信息
            if (conv.getTargetUserId() != null) {
                UserDTO targetUser = getUserInfo(conv.getTargetUserId());
                if (targetUser != null) {
                    dto.setTargetUsername(targetUser.getUsername());
                    dto.setTargetNickname(targetUser.getNickname());
                    dto.setTargetAvatar(targetUser.getAvatar());
                }
            }
            result.add(dto);
        }
        
        return result;
    }
    
    @Override
    public List<MessageDTO> getMessageList(Long conversationId, Long userId, Integer page, Integer size) {
        // 验证会话归属
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
               .orderByDesc(Message::getCreateTime);
        
        Page<Message> pageResult = messageMapper.selectPage(new Page<>(page, size), wrapper);
        List<MessageDTO> result = new ArrayList<>();
        
        for (Message msg : pageResult.getRecords()) {
            MessageDTO dto = convertToDTO(msg, userId);
            // 填充发送者信息
            UserDTO sender = getUserInfo(msg.getSenderId());
            if (sender != null) {
                dto.setSenderUsername(sender.getUsername());
                dto.setSenderNickname(sender.getNickname());
                dto.setSenderAvatar(sender.getAvatar());
            }
            result.add(dto);
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();
        
        // 获取或创建发送者的会话
        Conversation senderConv = getOrCreateConversationEntity(senderId, receiverId);
        // 获取或创建接收者的会话
        Conversation receiverConv = getOrCreateConversationEntity(receiverId, senderId);
        
        // 创建消息
        Message message = new Message();
        message.setConversationId(senderConv.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setType(request.getType());
        message.setContent(request.getContent());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setAttachmentName(request.getAttachmentName());
        message.setAttachmentSize(request.getAttachmentSize());
        message.setStatus(1); // 已送达
        message.setCreateTime(LocalDateTime.now());
        message.setDeleted(0);
        
        messageMapper.insert(message);
        
        // 为接收者也创建一条消息记录（关联到接收者的会话）
        Message receiverMessage = new Message();
        receiverMessage.setConversationId(receiverConv.getId());
        receiverMessage.setSenderId(senderId);
        receiverMessage.setReceiverId(receiverId);
        receiverMessage.setType(request.getType());
        receiverMessage.setContent(request.getContent());
        receiverMessage.setAttachmentUrl(request.getAttachmentUrl());
        receiverMessage.setAttachmentName(request.getAttachmentName());
        receiverMessage.setAttachmentSize(request.getAttachmentSize());
        receiverMessage.setStatus(1);
        receiverMessage.setCreateTime(LocalDateTime.now());
        receiverMessage.setDeleted(0);
        
        messageMapper.insert(receiverMessage);
        
        // 更新发送者会话
        updateConversationLastMessage(senderConv, message);
        
        // 更新接收者会话（增加未读数）
        receiverConv.setLastMessageId(receiverMessage.getId());
        receiverConv.setLastMessageContent(truncateContent(request.getContent()));
        receiverConv.setLastMessageTime(LocalDateTime.now());
        receiverConv.setUnreadCount(receiverConv.getUnreadCount() + 1);
        receiverConv.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(receiverConv);
        
        // 更新接收者的未读消息数缓存
        incrementUnreadCountCache(receiverId);
        
        // 转换为DTO
        MessageDTO dto = convertToDTO(message, senderId);
        UserDTO sender = getUserInfo(senderId);
        if (sender != null) {
            dto.setSenderUsername(sender.getUsername());
            dto.setSenderNickname(sender.getNickname());
            dto.setSenderAvatar(sender.getAvatar());
        }
        
        // 通过WebSocket推送消息给接收者
        webSocketHandler.sendMessageToUser(receiverId, dto);
        
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        
        // 更新会话未读数
        int oldUnread = conversation.getUnreadCount() != null ? conversation.getUnreadCount() : 0;
        conversation.setUnreadCount(0);
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        
        // 更新未读消息数缓存
        decrementUnreadCountCache(userId, oldUnread);
        
        // 更新消息状态为已读
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
               .eq(Message::getReceiverId, userId)
               .set(Message::getStatus, 2);
        messageMapper.update(null, wrapper);
    }
    
    @Override
    public void markMessageRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message != null && message.getReceiverId().equals(userId)) {
            message.setStatus(2);
            messageMapper.updateById(message);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        
        // 减少缓存中的未读数
        int unread = conversation.getUnreadCount() != null ? conversation.getUnreadCount() : 0;
        if (unread > 0) {
            decrementUnreadCountCache(userId, unread);
        }
        
        // 删除会话
        conversationMapper.deleteById(conversationId);
        
        // 删除会话下的消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId);
        messageMapper.delete(wrapper);
    }
    
    @Override
    public Integer getUnreadCount(Long userId) {
        String cacheKey = UNREAD_COUNT_PREFIX + userId;
        
        try {
            // 先从Redis缓存获取
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Integer.parseInt(cached);
            }
        } catch (Exception e) {
            log.warn("从Redis获取未读数失败: {}", e.getMessage());
        }
        
        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .select(Conversation::getUnreadCount);
        
        List<Conversation> conversations = conversationMapper.selectList(wrapper);
        int totalUnread = conversations.stream()
                .mapToInt(c -> c.getUnreadCount() != null ? c.getUnreadCount() : 0)
                .sum();
        
        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(totalUnread), UNREAD_COUNT_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入未读数缓存失败: {}", e.getMessage());
        }
        
        return totalUnread;
    }
    
    @Override
    public ConversationDTO getOrCreateConversation(Long userId, Long targetUserId) {
        Conversation conversation = getOrCreateConversationEntity(userId, targetUserId);
        ConversationDTO dto = convertToDTO(conversation);
        
        UserDTO targetUser = getUserInfo(targetUserId);
        if (targetUser != null) {
            dto.setTargetUsername(targetUser.getUsername());
            dto.setTargetNickname(targetUser.getNickname());
            dto.setTargetAvatar(targetUser.getAvatar());
        }
        
        return dto;
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 增加用户未读消息数缓存
     */
    private void incrementUnreadCountCache(Long userId) {
        String cacheKey = UNREAD_COUNT_PREFIX + userId;
        try {
            Long newValue = redisTemplate.opsForValue().increment(cacheKey);
            if (newValue != null && newValue == 1) {
                // 如果是新key，设置过期时间
                redisTemplate.expire(cacheKey, UNREAD_COUNT_TTL, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("增加未读数缓存失败: {}", e.getMessage());
            // 删除缓存，下次从数据库重新加载
            redisTemplate.delete(cacheKey);
        }
    }
    
    /**
     * 减少用户未读消息数缓存
     */
    private void decrementUnreadCountCache(Long userId, int count) {
        if (count <= 0) return;
        String cacheKey = UNREAD_COUNT_PREFIX + userId;
        try {
            Long current = redisTemplate.opsForValue().decrement(cacheKey, count);
            if (current != null && current < 0) {
                // 如果变成负数，重置为0
                redisTemplate.opsForValue().set(cacheKey, "0", UNREAD_COUNT_TTL, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("减少未读数缓存失败: {}", e.getMessage());
            redisTemplate.delete(cacheKey);
        }
    }
    
    private Conversation getOrCreateConversationEntity(Long userId, Long targetUserId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getTargetUserId, targetUserId);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            // 判断是否为好友（这里简化处理，默认为陌生人）
            conversation = new Conversation();
            conversation.setType(2); // 陌生人私聊
            conversation.setUserId(userId);
            conversation.setTargetUserId(targetUserId);
            conversation.setUnreadCount(0);
            conversation.setPinned(0);
            conversation.setMuted(0);
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversation.setDeleted(0);
            
            conversationMapper.insert(conversation);
        }
        return conversation;
    }
    
    private void updateConversationLastMessage(Conversation conversation, Message message) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(truncateContent(message.getContent()));
        conversation.setLastMessageTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }
    
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
    
    private UserDTO getUserInfo(Long userId) {
        if (userId == null) return null;
        
        String cacheKey = USER_CACHE_PREFIX + userId;
        
        try {
            // 先从Redis缓存获取
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(cached, UserDTO.class);
            }
            
            // 缓存未命中，从用户服务获取
            Result<UserDTO> result = userClient.getUserById(userId);
            if (result.getCode() == 200 && result.getData() != null) {
                UserDTO user = result.getData();
                // 存入Redis缓存
                redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(user), USER_CACHE_TTL, TimeUnit.MINUTES);
                return user;
            }
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", userId, e);
        }
        
        return null;
    }
    
    private ConversationDTO convertToDTO(Conversation conv) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(String.valueOf(conv.getId()));
        dto.setType(conv.getType());
        dto.setTargetUserId(conv.getTargetUserId() != null ? String.valueOf(conv.getTargetUserId()) : null);
        dto.setTitle(conv.getTitle());
        dto.setLastMessageContent(conv.getLastMessageContent());
        dto.setLastMessageTime(conv.getLastMessageTime());
        dto.setUnreadCount(conv.getUnreadCount());
        dto.setPinned(conv.getPinned() != null && conv.getPinned() == 1);
        dto.setMuted(conv.getMuted() != null && conv.getMuted() == 1);
        dto.setCreateTime(conv.getCreateTime());
        return dto;
    }
    
    private MessageDTO convertToDTO(Message msg, Long currentUserId) {
        MessageDTO dto = new MessageDTO();
        dto.setId(String.valueOf(msg.getId()));
        dto.setConversationId(String.valueOf(msg.getConversationId()));
        dto.setSenderId(String.valueOf(msg.getSenderId()));
        dto.setReceiverId(String.valueOf(msg.getReceiverId()));
        dto.setType(msg.getType());
        dto.setContent(msg.getContent());
        dto.setAttachmentUrl(msg.getAttachmentUrl());
        dto.setAttachmentName(msg.getAttachmentName());
        dto.setAttachmentSize(msg.getAttachmentSize());
        dto.setStatus(msg.getStatus());
        dto.setExtraData(msg.getExtraData());
        dto.setIsSelf(msg.getSenderId().equals(currentUserId));
        dto.setCreateTime(msg.getCreateTime());
        return dto;
    }
}
