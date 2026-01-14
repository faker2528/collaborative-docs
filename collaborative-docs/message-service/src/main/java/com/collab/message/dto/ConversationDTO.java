package com.collab.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话DTO
 */
@Data
public class ConversationDTO {
    
    private String id;
    
    /**
     * 会话类型: 1-好友私聊, 2-陌生人私聊, 3-系统消息, 4-申请消息
     */
    private Integer type;
    
    /**
     * 对方用户ID
     */
    private String targetUserId;
    
    /**
     * 对方用户名
     */
    private String targetUsername;
    
    /**
     * 对方昵称
     */
    private String targetNickname;
    
    /**
     * 对方头像
     */
    private String targetAvatar;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessageContent;
    
    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 未读消息数
     */
    private Integer unreadCount;
    
    /**
     * 是否置顶
     */
    private Boolean pinned;
    
    /**
     * 是否静音
     */
    private Boolean muted;
    
    private LocalDateTime createTime;
}
