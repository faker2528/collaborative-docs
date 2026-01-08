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
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 创建者ID
     */
    private Long creatorId;
    
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
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
