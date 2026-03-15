package com.collab.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.dto.CommentDTO;
import com.collab.common.dto.UserDTO;
import com.collab.common.entity.Comment;
import com.collab.common.entity.CommentLike;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.Result;
import com.collab.message.client.CollaborationClient;
import com.collab.message.client.UserClient;
import com.collab.message.mapper.CommentLikeMapper;
import com.collab.message.mapper.CommentMapper;
import com.collab.message.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserClient userClient;
    private final CollaborationClient collaborationClient;
    
    @Override
    public List<CommentDTO> getComments(String documentId, String userId) {
        log.info("获取文档评论列表，documentId: {}, userId: {}", documentId, userId);
        
        // 使用 LambdaQueryWrapper 查询评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocumentId, documentId)
               .orderByDesc(Comment::getCreateTime);
        
        List<Comment> comments = commentMapper.selectList(wrapper);
        
        // 批量获取用户信息并转换为 DTO
        return comments.stream()
                .map(comment -> {
                    CommentDTO dto = convertToDTO(comment);
                    // 检查当前用户是否已点赞该评论
                    LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
                    likeWrapper.eq(CommentLike::getCommentId, comment.getId())
                               .eq(CommentLike::getUserId, userId);
                    long liked = commentLikeMapper.selectCount(likeWrapper);
                    dto.setLiked(liked > 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO addComment(String documentId, String userId, String text, String parentId, String replyToId) {
        log.info("添加评论，documentId: {}, userId: {}, text: {}, parentId: {}, replyToId: {}", 
                documentId, userId, text, parentId, replyToId);
        
        Comment comment = new Comment();
        comment.setDocumentId(documentId);
        comment.setUserId(userId);
        comment.setText(text);
        comment.setParentId(parentId);
        comment.setReplyToId(replyToId);
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setDeleted(0);
        
        commentMapper.insert(comment);
        
        // 返回 DTO（包含用户名、头像和回复信息）
        Comment savedComment = commentMapper.selectById(comment.getId());
        CommentDTO dto = convertToDTO(savedComment);
        
        // 广播评论消息到 WebSocket
        broadcastComment(documentId, dto, parentId != null);
        
        return dto;
    }
    
    /**
     * 广播评论消息
     */
    private void broadcastComment(String documentId, CommentDTO comment, boolean isReply) {
        try {
            collaborationClient.broadcastComment(
                    documentId,
                    comment.getId(),
                    comment.getUserId(),
                    comment.getUsername(),
                    comment.getText(),
                    comment.getParentId(),
                    isReply
            );
            log.info("已广播评论消息，documentId: {}, commentId: {}", documentId, comment.getId());
        } catch (Exception e) {
            // 广播失败不影响评论保存
            log.error("广播评论消息失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(String commentId, String userId) {
        log.info("删除评论，commentId: {}, userId: {}", commentId, userId);
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 检查是否是自己的评论
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除该评论");
        }
        
        // 逻辑删除
        commentMapper.deleteById(commentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(String commentId, String userId) {
        log.info("点赞/取消点赞，commentId: {}, userId: {}", commentId, userId);
        
        // 检查评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CommentLike::getCommentId, commentId)
                   .eq(CommentLike::getUserId, userId);
        CommentLike existingLike = commentLikeMapper.selectOne(likeWrapper);
        
        if (existingLike != null) {
            // 已点赞，取消点赞
            commentLikeMapper.deleteById(existingLike.getId());
            
            // 更新点赞数（确保不会变成负数）
            int newCount = Math.max(0, comment.getLikeCount() - 1);
            comment.setLikeCount(newCount);
            commentMapper.updateById(comment);
            
            log.info("取消点赞成功，评论 ID: {}, 当前点赞数：{}", commentId, newCount);
        } else {
            // 未点赞，添加点赞
            CommentLike commentLike = new CommentLike();
            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);
            commentLike.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(commentLike);
            
            // 更新点赞数
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            
            log.info("点赞成功，评论 ID: {}, 当前点赞数：{}", commentId, comment.getLikeCount());
        }
    }
    
    /**
     * 转换为 DTO（包含用户信息）
     */
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        // 将 Long 类型转为 String，避免前端精度丢失
        dto.setId(comment.getId());
        dto.setDocumentId(comment.getDocumentId());
        dto.setUserId(comment.getUserId());
        dto.setText(comment.getText());
        dto.setParentId(comment.getParentId() != null ? comment.getParentId() : null);
        dto.setReplyToId(comment.getReplyToId() != null ? comment.getReplyToId() : null);
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreateTime(comment.getCreateTime());
        
        // 通过 Feign 客户端调用用户服务获取用户信息
        try {
            Result<UserDTO> result = userClient.getUserById(comment.getUserId());
            if (result != null && result.getData() != null) {
                UserDTO user = result.getData();
                dto.setUsername(user.getUsername());
                dto.setAvatarUrl(user.getAvatar());
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，userId: {}", comment.getUserId(), e);
            // 如果获取用户信息失败，使用默认值
            dto.setUsername("用户" + comment.getUserId());
        }
        
        // 如果有回复目标，获取回复目标的用户名
        if (comment.getReplyToId() != null && !comment.getReplyToId().equals(comment.getParentId())) {
            try {
                Comment replyToComment = commentMapper.selectById(comment.getReplyToId());
                if (replyToComment != null) {
                    Result<UserDTO> replyToUserResult = userClient.getUserById(replyToComment.getUserId());
                    if (replyToUserResult != null && replyToUserResult.getData() != null) {
                        dto.setReplyToUsername(replyToUserResult.getData().getUsername());
                    }
                }
            } catch (Exception e) {
                log.error("获取回复目标用户信息失败，replyToId: {}", comment.getReplyToId(), e);
            }
        }
        
        return dto;
    }
}
