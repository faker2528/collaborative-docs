package com.collab.common.dto;

import lombok.Data;

/**
 * 发送好友请求
 */
@Data
public class SendFriendRequest {
    /**
     * 目标用户ID
     */
    private String toUserId;
    
    /**
     * 附加消息
     */
    private String message;
}
