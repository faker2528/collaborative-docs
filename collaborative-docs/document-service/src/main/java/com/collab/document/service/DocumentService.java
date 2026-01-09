package com.collab.document.service;

import com.collab.common.dto.CreateDocumentRequest;
import com.collab.common.dto.DocumentDTO;
import com.collab.common.dto.DocumentMemberDTO;

import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {
    
    /**
     * 创建文档
     */
    DocumentDTO createDocument(CreateDocumentRequest request, Long userId);
    
    /**
     * 获取文档详情
     */
    DocumentDTO getDocument(Long documentId, Long userId);
    
    /**
     * 获取用户的文档列表
     */
    List<DocumentDTO> getUserDocuments(Long userId);
    
    /**
     * 更新文档内容
     */
    DocumentDTO updateDocument(Long documentId, String content, Long userId);
    
    /**
     * 删除文档
     */
    void deleteDocument(Long documentId, Long userId);
    
    /**
     * 分享文档给其他用户
     */
    void shareDocument(Long documentId, Long targetUserId, Integer permissionType, Long operatorId);
    
    /**
     * 检查用户对文档的权限
     */
    boolean hasPermission(Long documentId, Long userId, Integer requiredPermission);
    
    /**
     * 获取文档成员列表
     */
    List<DocumentMemberDTO> getDocumentMembers(Long documentId, Long userId);
    
    /**
     * 修改成员权限
     */
    void updateMemberPermission(Long documentId, Long targetUserId, Integer permissionType, Long operatorId);
    
    /**
     * 移除文档成员
     */
    void removeMember(Long documentId, Long targetUserId, Long operatorId);
}
