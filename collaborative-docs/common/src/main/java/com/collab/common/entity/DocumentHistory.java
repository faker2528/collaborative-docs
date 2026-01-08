package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档版本历史实体
 */
@Data
@TableName("t_document_history")
public class DocumentHistory implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 文档内容快照
     */
    private String content;
    
    /**
     * 操作类型: 1-创建, 2-编辑, 3-回滚
     */
    private Integer operationType;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 操作者ID
     */
    private Long operatorId;
    
    /**
     * 操作者名称
     */
    private String operatorName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
