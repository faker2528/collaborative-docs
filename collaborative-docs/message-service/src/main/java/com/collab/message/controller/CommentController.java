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
    public Result<List<CommentDTO>> getComments(@PathVariable("documentId") String documentId,
                                                 @RequestHeader("X-User-Id") String userId) {
        List<CommentDTO> comments = commentService.getComments(documentId, userId);
        return Result.success(comments);
    }
    
    /**
     * 添加评论
     */
    @PostMapping
    public Result<CommentDTO> addComment(@RequestBody Map<String, Object> request,
                                         @RequestHeader("X-User-Id") String userId) {
        String documentId = (String) request.get("documentId");
        String text = (String) request.get("text");
        String parentId = (String) request.get("parentId");
        String replyToId = (String) request.get("replyToId");
        
        CommentDTO comment = commentService.addComment(documentId, userId, text, parentId, replyToId);
        return Result.success("评论已发布", comment);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable("commentId") String commentId,
                                      @RequestHeader("X-User-Id") String userId) {
        commentService.deleteComment(commentId, userId);
        return Result.success("评论已删除", null);
    }
    
    /**
     * 回复评论
     */
    @PostMapping("/reply")
    public Result<CommentDTO> replyComment(@RequestBody Map<String, Object> request,
                                           @RequestHeader("X-User-Id") String userId) {
        String documentId = (String) request.get("documentId");
        String parentId = (String) request.get("parentId");
        String replyToId = request.get("replyToId") != null ? (String) request.get("replyToId") : parentId;
        String text = (String) request.get("text");
        
        CommentDTO comment = commentService.addComment(documentId, userId, text, parentId, replyToId);
        return Result.success("回复成功", comment);
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Result<Void> likeComment(@PathVariable("commentId") String commentId,
                                    @RequestHeader("X-User-Id") String userId) {
        commentService.likeComment(commentId, userId);
        return Result.success("点赞成功", null);
    }
}
