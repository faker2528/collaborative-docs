package com.collab.message.controller;

import com.collab.common.dto.CommentDTO;
import com.collab.common.result.Result;
import com.collab.message.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 获取文档评论列表
     */
    @GetMapping("/{documentId}/list")
    public Result<List<CommentDTO>> getComments(@PathVariable("documentId") Long documentId,
                                                 @RequestHeader("X-User-Id") Long userId) {
        List<CommentDTO> comments = commentService.getComments(documentId, userId);
        return Result.success(comments);
    }
    
    /**
     * 添加评论
     */
    @PostMapping
    public Result<CommentDTO> addComment(@RequestBody Map<String, Object> request,
                                         @RequestHeader("X-User-Id") Long userId) {
        Long documentId = Long.parseLong(request.get("documentId").toString());
        String text = (String) request.get("text");
        Long parentId = request.get("parentId") != null ? Long.parseLong(request.get("parentId").toString()) : null;
        Long replyToId = request.get("replyToId") != null ? Long.parseLong(request.get("replyToId").toString()) : null;
        
        CommentDTO comment = commentService.addComment(documentId, userId, text, parentId, replyToId);
        return Result.success("评论已发布", comment);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable("commentId") Long commentId,
                                      @RequestHeader("X-User-Id") Long userId) {
        commentService.deleteComment(commentId, userId);
        return Result.success("评论已删除", null);
    }
    
    /**
     * 回复评论
     */
    @PostMapping("/reply")
    public Result<CommentDTO> replyComment(@RequestBody Map<String, Object> request,
                                           @RequestHeader("X-User-Id") Long userId) {
        Long documentId = Long.parseLong(request.get("documentId").toString());
        Long parentId = Long.parseLong(request.get("parentId").toString());
        Long replyToId = request.get("replyToId") != null ? Long.parseLong(request.get("replyToId").toString()) : parentId;
        String text = (String) request.get("text");
        
        CommentDTO comment = commentService.addComment(documentId, userId, text, parentId, replyToId);
        return Result.success("回复成功", comment);
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Result<Void> likeComment(@PathVariable("commentId") Long commentId,
                                    @RequestHeader("X-User-Id") Long userId) {
        commentService.likeComment(commentId, userId);
        return Result.success("点赞成功", null);
    }
}
