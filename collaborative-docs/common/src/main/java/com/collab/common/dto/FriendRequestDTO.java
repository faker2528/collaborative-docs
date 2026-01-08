package com.collab.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求DTO
 */
@Data
public class FriendRequestDTO {
    private String id;
    private String fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private String toUserId;
    private String message;
    private Integer status;
    private LocalDateTime createTime;
}
