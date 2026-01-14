package com.collab.message.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {
    
    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    
    /**
     * 消息类型: 1-文字, 2-图片, 3-文件
     */
    private Integer type = 1;
    
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
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
     * 附件大小
     */
    private Long attachmentSize;
}
