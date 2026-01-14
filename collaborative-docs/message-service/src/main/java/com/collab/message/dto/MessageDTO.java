package com.collab.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息DTO
 */
@Data
public class MessageDTO {
    
    private String id;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送者用户名
     */
    private String senderUsername;
    
    /**
     * 发送者昵称
     */
    private String senderNickname;
    
    /**
     * 发送者头像
     */
    private String senderAvatar;
    
    /**
     * 接收者ID
     */
    private String receiverId;
    
    /**
     * 消息类型: 1-文字, 2-图片, 3-文件, 4-系统通知
     */
    private Integer type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 附件URL
     */
    private String attachmentUrl;
    
    /**
     * 附件名称
     */
    private String attachmentName;
    
    /**
     * 附件大小
     */
    private Long attachmentSize;
    
    /**
     * 消息状态: 0-发送中, 1-已送达, 2-已读
     */
    private Integer status;
    
    /**
     * 额外数据
     */
    private String extraData;
    
    /**
     * 是否是自己发送的
     */
    private Boolean isSelf;
    
    private LocalDateTime createTime;
}
