package com.collab.message.service;

import com.collab.message.dto.ConversationDTO;
import com.collab.message.dto.MessageDTO;
import com.collab.message.dto.SendMessageRequest;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    /**
     * 获取用户会话列表
     */
    List<ConversationDTO> getConversationList(String userId);
    
    /**
     * 获取会话消息列表
     */
    List<MessageDTO> getMessageList(String conversationId, String userId, Integer page, Integer size);
    
    /**
     * 发送消息
     */
    MessageDTO sendMessage(String senderId, SendMessageRequest request);
    
    /**
     * 标记会话已读
     */
    void markConversationRead(String conversationId, String userId);
    
    /**
     * 标记消息已读
     */
    void markMessageRead(String messageId, String userId);
    
    /**
     * 删除会话
     */
    void deleteConversation(String conversationId, String userId);
    
    /**
     * 获取未读消息总数
     */
    Integer getUnreadCount(String userId);
    
    /**
     * 获取或创建与目标用户的会话
     */
    ConversationDTO getOrCreateConversation(String userId, String targetUserId);
}
