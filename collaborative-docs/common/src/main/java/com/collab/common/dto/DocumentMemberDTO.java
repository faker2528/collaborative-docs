package com.collab.common.dto;

import lombok.Data;

/**
 * 文档成员DTO
 */
@Data
public class DocumentMemberDTO {
    
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer permissionType;  // 1-只读 2-可编辑 3-管理
    private Boolean isCreator;       // 是否是创建者
}
