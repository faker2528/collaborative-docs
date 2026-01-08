package com.collab.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.dto.CreateDocumentRequest;
import com.collab.common.dto.DocumentDTO;
import com.collab.common.entity.Document;
import com.collab.common.entity.DocumentPermission;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.ResultCode;
import com.collab.document.mapper.DocumentMapper;
import com.collab.document.mapper.DocumentPermissionMapper;
import com.collab.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentPermissionMapper permissionMapper;

    /**
     * 权限类型常量
     */
    private static final int PERMISSION_VIEW = 1;
    private static final int PERMISSION_EDIT = 2;
    private static final int PERMISSION_MANAGE = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO createDocument(CreateDocumentRequest request, Long userId) {
        log.debug("【Creating document】: title={}, userId={}", request.getTitle(), userId);
        
        // 创建文档
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent() != null ? request.getContent() : "");
        document.setCreatorId(userId);
        document.setVersion(1);
        document.setStatus(1);
        document.setDeleted(0);

        log.debug("【Before insert】: document id={}", document.getId());
        documentMapper.insert(document);
        log.debug("【After insert】: document id={}", document.getId());

        // 为创建者添加管理权限
        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(document.getId());
        permission.setUserId(userId);
        permission.setPermissionType(PERMISSION_MANAGE);
        permissionMapper.insert(permission);

        log.info("【Document created】: id={}, title={}, creator={}", document.getId(), document.getTitle(), userId);

        // 重新查询完整的文档信息
        Document createdDocument = documentMapper.selectById(document.getId());
        log.debug("【After select】: document id={}, title={}", createdDocument.getId(), createdDocument.getTitle());
        
        DocumentDTO result = convertToDTO(createdDocument, PERMISSION_MANAGE);
        log.debug("【Returning DTO】: id={}, title={}", result.getId(), result.getTitle());
        return result;
    }

    @Override
    public DocumentDTO getDocument(Long documentId, Long userId) {
        log.debug("【Get document】: id={}, userId={}", documentId, userId);

        Document document = null;
        for (int i = 0; i < 3; i++) {
            document = documentMapper.selectById(documentId);
            if (document != null) {
                break;
            }
            if (i < 2) { // 不是最后一次尝试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (document == null) {
            log.warn("【Document not found】: id={}", documentId);
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        log.debug("【Document found】: id={}, creatorId={}, userId={}", documentId, document.getCreatorId(), userId);
        
        // 检查权限 - 首先检查是否是创建者
        if (document.getCreatorId().equals(userId)) {
            log.debug("【User is creator, granting MANAGE permission】");
            return convertToDTO(document, PERMISSION_MANAGE);
        }
        
        // 然后检查权限表
        Integer permission = permissionMapper.getPermissionType(documentId, userId);
        log.debug("【Permission from table】: {}", permission);
        if (permission == null) {
            log.warn("【User has no permission to access document】: documentId={}, userId={}", documentId, userId);
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }

        return convertToDTO(document, permission);
    }

    @Override
    public List<DocumentDTO> getUserDocuments(Long userId) {
        List<DocumentDTO> result = new ArrayList<>();

        // 获取用户创建的文档
        LambdaQueryWrapper<Document> creatorWrapper = new LambdaQueryWrapper<>();
        creatorWrapper.eq(Document::getCreatorId, userId)
                .orderByDesc(Document::getUpdateTime);
        List<Document> createdDocs = documentMapper.selectList(creatorWrapper);
        
        for (Document doc : createdDocs) {
            result.add(convertToDTO(doc, PERMISSION_MANAGE));
        }

        // 获取用户被授权的文档
        LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(DocumentPermission::getUserId, userId);
        List<DocumentPermission> permissions = permissionMapper.selectList(permWrapper);

        for (DocumentPermission perm : permissions) {
            Document doc = documentMapper.selectById(perm.getDocumentId());
            if (doc != null && !doc.getCreatorId().equals(userId)) {
                result.add(convertToDTO(doc, perm.getPermissionType()));
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO updateDocument(Long documentId, String content, Long userId) {
        // 检查编辑权限
        if (!hasPermission(documentId, userId, PERMISSION_EDIT)) {
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }

        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        document.setContent(content);
        document.setVersion(document.getVersion() + 1);
        documentMapper.updateById(document);

        log.info("Document updated: id={}, version={}", documentId, document.getVersion());

        return convertToDTO(document, getPermission(documentId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId, Long userId) {
        // 检查管理权限
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 只有创建者可以删除
        if (!document.getCreatorId().equals(userId)) {
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }

        documentMapper.deleteById(documentId);

        // 删除相关权限
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId);
        permissionMapper.delete(wrapper);

        log.info("Document deleted: id={}", documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareDocument(Long documentId, Long targetUserId, Integer permissionType, Long operatorId) {
        // 检查操作者是否有管理权限
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        if (!document.getCreatorId().equals(operatorId) && !hasPermission(documentId, operatorId, PERMISSION_MANAGE)) {
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }

        // 检查是否已有权限记录
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
                .eq(DocumentPermission::getUserId, targetUserId);
        DocumentPermission existing = permissionMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setPermissionType(permissionType);
            permissionMapper.updateById(existing);
        } else {
            DocumentPermission permission = new DocumentPermission();
            permission.setDocumentId(documentId);
            permission.setUserId(targetUserId);
            permission.setPermissionType(permissionType);
            permissionMapper.insert(permission);
        }

        log.info("Document shared: docId={}, targetUser={}, permission={}", documentId, targetUserId, permissionType);
    }

    @Override
    public boolean hasPermission(Long documentId, Long userId, Integer requiredPermission) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return false;
        }

        // 创建者拥有所有权限
        if (document.getCreatorId().equals(userId)) {
            return true;
        }

        Integer userPermission = permissionMapper.getPermissionType(documentId, userId);
        return userPermission != null && userPermission >= requiredPermission;
    }

    /**
     * 获取用户对文档的权限
     */
    private Integer getPermission(Long documentId, Long userId) {
        // 首先检查是否是创建者
        Document document = documentMapper.selectById(documentId);
        if (document != null && document.getCreatorId().equals(userId)) {
            return PERMISSION_MANAGE;
        }
        // 否则查询权限表
        return permissionMapper.getPermissionType(documentId, userId);
    }

    /**
     * 转换为DTO
     */
    private DocumentDTO convertToDTO(Document document, Integer permissionType) {
        DocumentDTO dto = new DocumentDTO();
        // 将ID转换为字符串以避免前端精度问题
        dto.setId(String.valueOf(document.getId()));
        dto.setTitle(document.getTitle());
        dto.setContent(document.getContent());
        dto.setCreatorId(String.valueOf(document.getCreatorId()));
        dto.setVersion(document.getVersion());
        dto.setStatus(document.getStatus());
        dto.setCreateTime(document.getCreateTime());
        dto.setUpdateTime(document.getUpdateTime());
        dto.setPermissionType(permissionType);
        return dto;
    }
}
