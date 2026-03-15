package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档分享链接实体
 */
@Data
@TableName("t_share_link")
public class ShareLink implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 分享令牌
     */
    private String token;
    
    /**
     * 权限类型: 1-查看, 2-编辑
     */
    private Integer permissionType;
    
    /**
     * 过期时间(空表示永不过期)
     */
    private LocalDateTime expireTime;
    
    /**
     * 最大使用次数(0表示无限制)
     */
    private Integer maxUses;
    
    /**
     * 已使用次数
     */
    private Integer usedCount;
    
    /**
     * 状态: 0-已禁用, 1-正常
     */
    private Integer status;
    
    /**
     * 验证类型: 0-免验证, 1-密码验证, 2-邮箱验证
     */
    private Integer verificationType;
    
    /**
     * 访问密码（加密存储）
     */
    private String password;
    
    /**
     * 指定邮箱（当verificationType为2时使用）
     */
    private String email;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
