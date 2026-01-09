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
                                               @RequestHeader("X-User-Id") Long userId) {
        DocumentDTO document = documentService.createDocument(request, userId);
        return Result.success("文档创建成功", document);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public Result<DocumentDTO> getDocument(@PathVariable("documentId") Long documentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        DocumentDTO document = documentService.getDocument(documentId, userId);
        return Result.success(document);
    }

    /**
     * 获取用户的文档列表
     */
    @GetMapping("/list")
    public Result<List<DocumentDTO>> getUserDocuments(@RequestHeader("X-User-Id") Long userId) {
        List<DocumentDTO> documents = documentService.getUserDocuments(userId);
        return Result.success(documents);
    }

    /**
     * 更新文档内容
     */
    @PutMapping("/{documentId}")
    public Result<DocumentDTO> updateDocument(@PathVariable("documentId") Long documentId,
                                              @RequestBody String content,
                                              @RequestHeader("X-User-Id") Long userId) {
        DocumentDTO document = documentService.updateDocument(documentId, content, userId);
        return Result.success("文档更新成功", document);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public Result<Void> deleteDocument(@PathVariable("documentId") Long documentId,
                                       @RequestHeader("X-User-Id") Long userId) {
        documentService.deleteDocument(documentId, userId);
        return Result.success("文档删除成功", null);
    }

    /**
     * 分享文档
     */
    @PostMapping("/{documentId}/share")
    public Result<Void> shareDocument(@PathVariable("documentId") Long documentId,
                                      @RequestParam("targetUserId") Long targetUserId,
                                      @RequestParam("permissionType") Integer permissionType,
                                      @RequestHeader("X-User-Id") Long userId) {
        documentService.shareDocument(documentId, targetUserId, permissionType, userId);
        return Result.success("文档分享成功", null);
    }

    /**
     * 检查用户权限
     */
    @GetMapping("/{documentId}/permission")
    public Result<Boolean> checkPermission(@PathVariable("documentId") Long documentId,
                                           @RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(defaultValue = "1") Integer requiredPermission) {
        boolean hasPermission = documentService.hasPermission(documentId, userId, requiredPermission);
        return Result.success(hasPermission);
    }
    
    /**
     * 获取文档成员列表
     */
    @GetMapping("/{documentId}/members")
    public Result<List<DocumentMemberDTO>> getDocumentMembers(@PathVariable("documentId") Long documentId,
                                                              @RequestHeader("X-User-Id") Long userId) {
        List<DocumentMemberDTO> members = documentService.getDocumentMembers(documentId, userId);
        return Result.success(members);
    }
    
    /**
     * 修改成员权限
     */
    @PutMapping("/{documentId}/members/{targetUserId}")
    public Result<Void> updateMemberPermission(@PathVariable("documentId") Long documentId,
                                               @PathVariable("targetUserId") Long targetUserId,
                                               @RequestParam("permissionType") Integer permissionType,
                                               @RequestHeader("X-User-Id") Long userId) {
        documentService.updateMemberPermission(documentId, targetUserId, permissionType, userId);
        return Result.success("权限修改成功", null);
    }
    
    /**
     * 移除文档成员
     */
    @DeleteMapping("/{documentId}/members/{targetUserId}")
    public Result<Void> removeMember(@PathVariable("documentId") Long documentId,
                                     @PathVariable("targetUserId") Long targetUserId,
                                     @RequestHeader("X-User-Id") Long userId) {
        documentService.removeMember(documentId, targetUserId, userId);
        return Result.success("成员移除成功", null);
    }
}
