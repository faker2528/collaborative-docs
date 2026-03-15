package com.collab.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 DTO
 */
@Data
public class CommentDTO {
    
    /**
     * 评论 ID
     */
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
     * 用户名
     */
    private String username;
    
    /**
     * 用户头像
     */
    private String avatarUrl;
    
    /**
     * 评论内容
     */
    private String text;
    
    /**
     * 父评论 ID（如果是回复）
     */
    private String parentId;
    
    /**
     * 回复的目标评论 ID（用于标识回复的是哪条评论）
     */
    private String replyToId;
    
    /**
     * 回复的目标用户名（用于显示 "回复 @xxx"）
     */
    private String replyToUsername;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 是否已点赞
     */
    private Boolean liked;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
