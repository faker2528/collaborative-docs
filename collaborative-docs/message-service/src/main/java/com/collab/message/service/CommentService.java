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
    List<CommentDTO> getComments(Long documentId, Long userId);
    
    /**
     * 添加评论
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param text 评论内容
     * @param parentId 父评论ID（顶级评论ID，用于分组）
     * @param replyToId 回复的目标评论ID（用于标识回复的是哪条评论）
     */
    CommentDTO addComment(Long documentId, Long userId, String text, Long parentId, Long replyToId);
    
    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);
    
    /**
     * 点赞评论
     */
    void likeComment(Long commentId, Long userId);
}
