package com.collab.collaboration.controller;

import com.collab.collaboration.handler.CollaborationWebSocketHandler;
import com.collab.collaboration.model.WebSocketMessage;
import com.collab.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 协作内部 API 控制器（供其他微服务调用）
 */
@Slf4j
@RestController
@RequestMapping("/internal/collaboration")
@RequiredArgsConstructor
public class CollaborationInternalController {

    private final CollaborationWebSocketHandler webSocketHandler;

    /**
     * 广播评论消息
     */
    @PostMapping("/broadcast-comment")
    public Result<Void> broadcastComment(
            @RequestParam String documentId,
            @RequestParam String commentId,
            @RequestParam String userId,
            @RequestParam String username,
            @RequestParam String text,
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "false") boolean isReply
    ) {
        try {
            WebSocketMessage.CommentData commentData = new WebSocketMessage.CommentData(
                    commentId, documentId, userId, username, text, parentId
            );
            
            webSocketHandler.broadcastComment(documentId, commentData, isReply);
            
            return Result.success();
        } catch (NumberFormatException e) {
            log.error("Invalid ID format: {}", e.getMessage());
            return Result.error("ID 格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to broadcast comment: {}", e.getMessage(), e);
            return Result.error("广播评论失败: " + e.getMessage());
        }
    }
}
