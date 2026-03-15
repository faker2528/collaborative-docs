package com.collab.message.client;

import com.collab.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 协作服务Feign客户端
 */
@FeignClient(name = "gateway-service", path = "/internal/collaboration")
public interface CollaborationClient {
    
    /**
     * 广播评论消息
     */
    @PostMapping("/broadcast-comment")
    Result<Void> broadcastComment(
            @RequestParam("documentId") String documentId,
            @RequestParam("commentId") String commentId,
            @RequestParam("userId") String userId,
            @RequestParam("username") String username,
            @RequestParam("text") String text,
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam("isReply") boolean isReply
    );
}
