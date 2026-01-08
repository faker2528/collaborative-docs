package com.collab.common.dto;

import lombok.Data;

/**
 * 创建分享链接请求
 */
@Data
public class CreateShareLinkRequest {
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 权限类型: 1-查看, 2-编辑
     */
    private Integer permissionType;
    
    /**
     * 有效天数(0表示永不过期)
     */
    private Integer validDays;
    
    /**
     * 最大使用次数(0表示无限制)
     */
    private Integer maxUses;
}
