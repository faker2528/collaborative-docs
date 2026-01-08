package com.collab.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档DTO
 */
@Data
public class DocumentDTO {
    
    private String id;
    private String title;
    private String content;
    private String creatorId;
    private String creatorName;
    private Integer version;
    private Integer status;
    private Integer permissionType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
