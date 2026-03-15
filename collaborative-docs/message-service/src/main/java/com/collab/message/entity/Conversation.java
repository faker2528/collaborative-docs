package com.collab.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@TableName("t_conversation")
public class Conversation {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 会话类型: 1-好友私聊, 2-陌生人私聊, 3-系统消息, 4-申请消息
     */
    private Integer type;
    
    /**
     * 用户ID（会话所属用户）
     */
    private String userId;
    
    /**
     * 对方用户ID（私聊时）
     */
    private String targetUserId;
    
    /**
     * 会话标题（系统消息时使用）
     */
    private String title;
    
    /**
     * 最后一条消息ID
     */
    private String lastMessageId;
    
    /**
     * 最后一条消息内容摘要
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
    private Integer pinned;
    
    /**
     * 是否静音
     */
    private Integer muted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
