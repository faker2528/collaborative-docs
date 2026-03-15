package com.collab.document.controller;

import com.collab.common.dto.CreateDocumentRequest;
import com.collab.common.dto.DocumentDTO;
import com.collab.common.dto.DocumentMemberDTO;
import com.collab.common.result.Result;
import com.collab.document.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @PostMapping
    public Result<DocumentDTO> createDocument(@Valid @RequestBody CreateDocumentRequest request,
                                               @RequestHeader("X-User-Id") String userId) {
        DocumentDTO document = documentService.createDocument(request, userId);
        return Result.success("文档创建成功", document);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public Result<DocumentDTO> getDocument(@PathVariable("documentId") String documentId,
                                           @RequestHeader("X-User-Id") String userId) {
        DocumentDTO document = documentService.getDocument(documentId, userId);
        return Result.success(document);
    }

    /**
     * 获取用户的文档列表
     */
    @GetMapping("/list")
    public Result<List<DocumentDTO>> getUserDocuments(@RequestHeader("X-User-Id") String userId) {
        List<DocumentDTO> documents = documentService.getUserDocuments(userId);
        return Result.success(documents);
    }

    /**
     * 更新文档内容
     */
    @PutMapping("/{documentId}")
    public Result<DocumentDTO> updateDocument(@PathVariable("documentId") String documentId,
                                              @RequestBody String content,
                                              @RequestHeader("X-User-Id") String userId) {
        DocumentDTO document = documentService.updateDocument(documentId, content, userId);
        return Result.success("文档更新成功", document);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public Result<Void> deleteDocument(@PathVariable("documentId") String documentId,
                                       @RequestHeader("X-User-Id") String userId) {
        documentService.deleteDocument(documentId, userId);
        return Result.success("文档删除成功", null);
    }

    /**
     * 分享文档
     */
    @PostMapping("/{documentId}/share")
    public Result<Void> shareDocument(@PathVariable("documentId") String documentId,
                                      @RequestParam("targetUserId") String targetUserId,
                                      @RequestParam("permissionType") Integer permissionType,
                                      @RequestHeader("X-User-Id") String userId) {
        documentService.shareDocument(documentId, targetUserId, permissionType, userId);
        return Result.success("文档分享成功", null);
    }

    /**
     * 检查用户权限
     */
    @GetMapping("/{documentId}/permission")
    public Result<Boolean> checkPermission(@PathVariable("documentId") String documentId,
                                           @RequestHeader("X-User-Id") String userId,
                                           @RequestParam(defaultValue = "1") Integer requiredPermission) {
        boolean hasPermission = documentService.hasPermission(documentId, userId, requiredPermission);
        return Result.success(hasPermission);
    }
    
    /**
     * 获取文档成员列表
     */
    @GetMapping("/{documentId}/members")
    public Result<List<DocumentMemberDTO>> getDocumentMembers(@PathVariable("documentId") String documentId,
                                                              @RequestHeader("X-User-Id") String userId) {
        List<DocumentMemberDTO> members = documentService.getDocumentMembers(documentId, userId);
        return Result.success(members);
    }
    
    /**
     * 修改成员权限
     */
    @PutMapping("/{documentId}/members/{targetUserId}")
    public Result<Void> updateMemberPermission(@PathVariable("documentId") String documentId,
                                               @PathVariable("targetUserId") String targetUserId,
                                               @RequestParam("permissionType") Integer permissionType,
                                               @RequestHeader("X-User-Id") String userId) {
        documentService.updateMemberPermission(documentId, targetUserId, permissionType, userId);
        return Result.success("权限修改成功", null);
    }
    
    /**
     * 移除文档成员
     */
    @DeleteMapping("/{documentId}/members/{targetUserId}")
    public Result<Void> removeMember(@PathVariable("documentId") String documentId,
                                     @PathVariable("targetUserId") String targetUserId,
                                     @RequestHeader("X-User-Id") String userId) {
        documentService.removeMember(documentId, targetUserId, userId);
        return Result.success("成员移除成功", null);
    }
}
