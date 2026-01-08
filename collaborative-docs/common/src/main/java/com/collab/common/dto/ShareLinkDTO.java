package com.collab.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享链接DTO
 */
@Data
public class ShareLinkDTO {
    private String id;
    private String documentId;
    private String documentTitle;
    private String token;
    private String shareUrl;
    private Integer permissionType;
    private LocalDateTime expireTime;
    private Integer maxUses;
    private Integer usedCount;
    private Integer status;
    private LocalDateTime createTime;
}
