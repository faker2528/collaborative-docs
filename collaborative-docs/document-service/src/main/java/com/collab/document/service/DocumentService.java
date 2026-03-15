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
    DocumentDTO createDocument(CreateDocumentRequest request, String userId);
    
    /**
     * 获取文档详情
     */
    DocumentDTO getDocument(String documentId, String userId);
    
    /**
     * 获取用户的文档列表
     */
    List<DocumentDTO> getUserDocuments(String userId);
    
    /**
     * 更新文档内容
     */
    DocumentDTO updateDocument(String documentId, String content, String userId);
    
    /**
     * 删除文档
     */
    void deleteDocument(String documentId, String userId);
    
    /**
     * 分享文档给其他用户
     */
    void shareDocument(String documentId, String targetUserId, Integer permissionType, String operatorId);
    
    /**
     * 检查用户对文档的权限
     */
    boolean hasPermission(String documentId, String userId, Integer requiredPermission);
    
    /**
     * 获取文档成员列表
     */
    List<DocumentMemberDTO> getDocumentMembers(String documentId, String userId);
    
    /**
     * 修改成员权限
     */
    void updateMemberPermission(String documentId, String targetUserId, Integer permissionType, String operatorId);
    
    /**
     * 移除文档成员
     */
    void removeMember(String documentId, String targetUserId, String operatorId);
}
