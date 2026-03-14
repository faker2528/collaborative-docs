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
    
    /**
     * 验证类型: 0-免验证, 1-密码验证, 2-邮箱验证
     */
    private Integer verificationType;
    
    /**
     * 访问密码（当verificationType为1时使用）
     */
    private String password;
    
    /**
     * 指定邮箱（当verificationType为2时使用）
     */
    private String email;
}
