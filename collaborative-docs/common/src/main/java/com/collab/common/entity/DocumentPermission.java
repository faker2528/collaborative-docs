package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档权限实体
 */
@Data
@TableName("t_document_permission")
public class DocumentPermission implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 权限类型: 1-查看, 2-编辑, 3-管理
     */
    private Integer permissionType;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
