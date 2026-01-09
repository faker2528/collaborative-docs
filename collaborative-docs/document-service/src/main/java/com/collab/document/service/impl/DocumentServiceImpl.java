package com.collab.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.dto.CreateDocumentRequest;
import com.collab.common.dto.DocumentDTO;
import com.collab.common.dto.DocumentMemberDTO;
import com.collab.common.dto.UserDTO;
import com.collab.common.entity.Document;
import com.collab.common.entity.DocumentPermission;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.Result;
import com.collab.common.result.ResultCode;
import com.collab.document.client.UserClient;
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
    private final UserClient userClient;

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
        // 检查操作者是否有编辑权限（至少要能编辑才能邀请他人）
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        boolean isCreator = document.getCreatorId().equals(operatorId);
        boolean hasManagePermission = hasPermission(documentId, operatorId, PERMISSION_MANAGE);
        boolean hasEditPermission = hasPermission(documentId, operatorId, PERMISSION_EDIT);
        
        // 至少需要编辑权限才能邀请他人
        if (!isCreator && !hasEditPermission) {
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }

        // 核心逻辑：只有创建者才能赋予可编辑/管理权限
        // 非创建者邀请的用户默认只读权限
        int finalPermission = permissionType;
        if (!isCreator && !hasManagePermission) {
            // 非创建者/非管理员只能邀请只读用户
            finalPermission = PERMISSION_VIEW;
            log.info("Non-creator {} inviting user, forcing read-only permission", operatorId);
        }

        // 检查是否已有权限记录
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
                .eq(DocumentPermission::getUserId, targetUserId);
        DocumentPermission existing = permissionMapper.selectOne(wrapper);

        if (existing != null) {
            // 只有创建者才能修改权限
            if (isCreator || hasManagePermission) {
                existing.setPermissionType(finalPermission);
                permissionMapper.updateById(existing);
            }
            // 非创建者不能修改已有权限
        } else {
            DocumentPermission permission = new DocumentPermission();
            permission.setDocumentId(documentId);
            permission.setUserId(targetUserId);
            permission.setPermissionType(finalPermission);
            permissionMapper.insert(permission);
        }

        log.info("Document shared: docId={}, targetUser={}, permission={}, operator={}", 
                documentId, targetUserId, finalPermission, operatorId);
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
    
    @Override
    public List<DocumentMemberDTO> getDocumentMembers(Long documentId, Long userId) {
        // 检查用户是否有权限查看成员列表
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        
        if (!hasPermission(documentId, userId, PERMISSION_VIEW)) {
            throw new BusinessException(ResultCode.DOCUMENT_NO_PERMISSION);
        }
        
        List<DocumentMemberDTO> members = new ArrayList<>();
        
        // 添加创建者
        DocumentMemberDTO creator = new DocumentMemberDTO();
        creator.setUserId(String.valueOf(document.getCreatorId()));
        creator.setIsCreator(true);
        creator.setPermissionType(PERMISSION_MANAGE);
        
        // 获取创建者用户信息
        try {
            Result<UserDTO> creatorResult = userClient.getUserById(document.getCreatorId());
            if (creatorResult != null && creatorResult.getCode() == 200 && creatorResult.getData() != null) {
                creator.setUsername(creatorResult.getData().getUsername());
                creator.setNickname(creatorResult.getData().getNickname());
                creator.setAvatar(creatorResult.getData().getAvatar());
            }
        } catch (Exception e) {
            log.warn("Failed to get creator info: {}", e.getMessage());
        }
        members.add(creator);
        
        // 获取其他成员
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId);
        List<DocumentPermission> permissions = permissionMapper.selectList(wrapper);
        
        for (DocumentPermission perm : permissions) {
            // 跳过创建者（已经添加）
            if (perm.getUserId().equals(document.getCreatorId())) {
                continue;
            }
            
            DocumentMemberDTO member = new DocumentMemberDTO();
            member.setUserId(String.valueOf(perm.getUserId()));
            member.setPermissionType(perm.getPermissionType());
            member.setIsCreator(false);
            
            // 获取用户信息
            try {
                Result<UserDTO> userResult = userClient.getUserById(perm.getUserId());
                if (userResult != null && userResult.getCode() == 200 && userResult.getData() != null) {
                    member.setUsername(userResult.getData().getUsername());
                    member.setNickname(userResult.getData().getNickname());
                    member.setAvatar(userResult.getData().getAvatar());
                }
            } catch (Exception e) {
                log.warn("Failed to get user info for {}: {}", perm.getUserId(), e.getMessage());
            }
            members.add(member);
        }
        
        return members;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberPermission(Long documentId, Long targetUserId, Integer permissionType, Long operatorId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有创建者才能修改权限
        if (!document.getCreatorId().equals(operatorId)) {
            throw new BusinessException("只有文档创建者才能修改成员权限");
        }
        
        // 不能修改创建者自己的权限
        if (targetUserId.equals(operatorId)) {
            throw new BusinessException("不能修改自己的权限");
        }
        
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
               .eq(DocumentPermission::getUserId, targetUserId);
        DocumentPermission permission = permissionMapper.selectOne(wrapper);
        
        if (permission == null) {
            throw new BusinessException("该用户不是文档成员");
        }
        
        permission.setPermissionType(permissionType);
        permissionMapper.updateById(permission);
        
        log.info("Updated member permission: docId={}, userId={}, permission={}", 
                documentId, targetUserId, permissionType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long documentId, Long targetUserId, Long operatorId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        
        // 只有创建者才能移除成员
        if (!document.getCreatorId().equals(operatorId)) {
            throw new BusinessException("只有文档创建者才能移除成员");
        }
        
        // 不能移除创建者
        if (targetUserId.equals(document.getCreatorId())) {
            throw new BusinessException("不能移除文档创建者");
        }
        
        LambdaQueryWrapper<DocumentPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentPermission::getDocumentId, documentId)
               .eq(DocumentPermission::getUserId, targetUserId);
        
        int deleted = permissionMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException("该用户不是文档成员");
        }
        
        log.info("Removed member from document: docId={}, userId={}", documentId, targetUserId);
    }
}
