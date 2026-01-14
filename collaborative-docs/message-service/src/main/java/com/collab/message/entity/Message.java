package com.collab.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@TableName("t_message")
public class Message {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 会话ID
     */
    private Long conversationId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 消息类型: 1-文字, 2-图片, 3-文件, 4-系统通知
     */
    private Integer type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 附件URL（图片、文件时使用）
     */
    private String attachmentUrl;
    
    /**
     * 附件名称
     */
    private String attachmentName;
    
    /**
     * 附件大小(字节)
     */
    private Long attachmentSize;
    
    /**
     * 消息状态: 0-发送中, 1-已送达, 2-已读
     */
    private Integer status;
    
    /**
     * 额外数据（JSON格式，用于存储申请类消息的额外信息）
     */
    private String extraData;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
