package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@TableName("t_document")
public class Document implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档内容（富文本JSON）
     */
    private String content;
    
    /**
     * 创建者ID
     */
    private Long creatorId;
    
    /**
     * 当前版本号
     */
    private Integer version;
    
    /**
     * 状态: 0-草稿, 1-已发布, 2-已归档
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
