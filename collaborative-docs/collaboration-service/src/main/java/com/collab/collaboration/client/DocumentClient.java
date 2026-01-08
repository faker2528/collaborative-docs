package com.collab.collaboration.client;

import com.collab.common.dto.DocumentDTO;
import com.collab.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 文档服务 Feign 客户端
 */
@FeignClient(name = "document-service", path = "/document")
public interface DocumentClient {
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    Result<DocumentDTO> getDocument(@PathVariable("documentId") Long documentId,
                                    @RequestHeader("X-User-Id") Long userId);
    
    /**
     * 更新文档内容
     */
    @PutMapping("/{documentId}")
    Result<DocumentDTO> updateDocument(@PathVariable("documentId") Long documentId,
                                       @RequestBody String content,
                                       @RequestHeader("X-User-Id") Long userId);
}
