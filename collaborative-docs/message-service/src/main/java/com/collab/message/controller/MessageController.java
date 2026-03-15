package com.collab.message.controller;

import com.collab.common.result.Result;
import com.collab.message.dto.ConversationDTO;
import com.collab.message.dto.MessageDTO;
import com.collab.message.dto.SendMessageRequest;
import com.collab.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> getConversationList(
            @RequestHeader(value = "X-User-Id") String userId) {
        List<ConversationDTO> list = messageService.getConversationList(userId);
        return Result.success(list);
    }
    
    /**
     * 获取或创建与目标用户的会话
     */
    @GetMapping("/conversation/with/{targetUserId}")
    public Result<ConversationDTO> getOrCreateConversation(
            @RequestHeader(value = "X-User-Id") String userId,
            @PathVariable(value = "targetUserId") String targetUserId) {
        ConversationDTO conversation = messageService.getOrCreateConversation(userId, targetUserId);
        return Result.success(conversation);
    }
    
    /**
     * 获取会话消息列表
     */
    @GetMapping("/conversation/{conversationId}/messages")
    public Result<List<MessageDTO>> getMessageList(
            @PathVariable(value = "conversationId") String conversationId,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        List<MessageDTO> list = messageService.getMessageList(conversationId, userId, page, size);
        return Result.success(list);
    }
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<MessageDTO> sendMessage(
            @RequestHeader(value = "X-User-Id") String userId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageDTO message = messageService.sendMessage(userId, request);
        return Result.success("发送成功", message);
    }
    
    /**
     * 标记会话已读
     */
    @PostMapping("/conversation/{conversationId}/read")
    public Result<Void> markConversationRead(
            @PathVariable(value = "conversationId") String conversationId,
            @RequestHeader(value = "X-User-Id") String userId) {
        messageService.markConversationRead(conversationId, userId);
        return Result.success("已标记为已读", null);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> deleteConversation(
            @PathVariable(value = "conversationId") String conversationId,
            @RequestHeader(value = "X-User-Id") String userId) {
        messageService.deleteConversation(conversationId, userId);
        return Result.success("会话已删除", null);
    }
    
    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread/count")
    public Result<Map<String, Integer>> getUnreadCount(
            @RequestHeader(value = "X-User-Id") String userId) {
        Integer count = messageService.getUnreadCount(userId);
        return Result.success(Map.of("count", count));
    }
}
