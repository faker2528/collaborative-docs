package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
@TableName("t_comment")
public class Comment {
    
    /**
     * 评论 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;
    
    /**
     * 文档 ID
     */
    private String documentId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 评论内容
     */
    private String text;
    
    /**
     * 父评论 ID（顶级评论ID，用于分组）
     */
    private String parentId;
    
    /**
     * 回复的目标评论 ID（用于标识回复的是哪条评论）
     */
    private String replyToId;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否已删除
     */
    @TableLogic
    private Integer deleted;
}
