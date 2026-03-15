package com.collab.message.service;

import com.collab.common.dto.CommentDTO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 获取文档评论列表
     */
    List<CommentDTO> getComments(String documentId, String userId);
    
    /**
     * 添加评论
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param text 评论内容
     * @param parentId 父评论ID（顶级评论ID，用于分组）
     * @param replyToId 回复的目标评论ID（用于标识回复的是哪条评论）
     */
    CommentDTO addComment(String documentId, String userId, String text, String parentId, String replyToId);
    
    /**
     * 删除评论
     */
    void deleteComment(String commentId, String userId);
    
    /**
     * 点赞评论
     */
    void likeComment(String commentId, String userId);
}
