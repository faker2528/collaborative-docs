package com.collab.document.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.dto.CreateShareLinkRequest;
import com.collab.common.dto.ShareLinkDTO;
import com.collab.common.entity.Document;
import com.collab.common.entity.DocumentPermission;
import com.collab.common.entity.ShareLink;
import com.collab.common.exception.BusinessException;
import com.collab.document.mapper.DocumentMapper;
import com.collab.document.mapper.DocumentPermissionMapper;
import com.collab.document.mapper.ShareLinkMapper;
import com.collab.document.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分享链接服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkServiceImpl implements ShareLinkService {
    
    private final ShareLinkMapper shareLinkMapper;
    private final DocumentMapper documentMapper;
    private final DocumentPermissionMapper permissionMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareLinkDTO createShareLink(CreateShareLinkRequest request, Long userId) {
        // 解析documentId
        Long documentId = Long.parseLong(request.getDocumentId());
        
        // 验证文档存在
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        
        // 检查用户是否有权限访问该文档
        boolean isCreator = document.getCreatorId().equals(userId);
        if (!isCreator) {
            LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(DocumentPermission::getDocumentId, documentId)
                       .eq(DocumentPermission::getUserId, userId);
            if (permissionMapper.selectCount(permWrapper) == 0) {
                throw new BusinessException("无权操作此文档");
            }
        }
        
        // 非创建者只能生成只读链接
        int permissionType = request.getPermissionType() != null ? request.getPermissionType() : 1;
        if (!isCreator) {
            permissionType = 1; // 强制只读
            log.info("Non-creator {} generating share link, forcing read-only permission", userId);
        }
        
        // 生成唯一token
        String token = RandomUtil.randomString(32);
        
        // 创建分享链接
        ShareLink shareLink = new ShareLink();
        shareLink.setDocumentId(documentId);
        shareLink.setCreatorId(userId);
        shareLink.setToken(token);
        shareLink.setPermissionType(permissionType);
        
        // 设置过期时间
        if (request.getValidDays() != null && request.getValidDays() > 0) {
            shareLink.setExpireTime(LocalDateTime.now().plusDays(request.getValidDays()));
        }
        
        shareLink.setMaxUses(request.getMaxUses() != null ? request.getMaxUses() : 0);
        shareLink.setUsedCount(0);
        shareLink.setStatus(1);
        
        shareLinkMapper.insert(shareLink);
        
        log.info("Created share link for document {} by user {}, permission: {}", documentId, userId, permissionType);
        
        return convertToDTO(shareLink, document.getTitle());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinByShareLink(String token, Long userId) {
        // 查找链接
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null) {
            throw new BusinessException("分享链接不存在");
        }
        if (shareLink.getStatus() != 1) {
            throw new BusinessException("分享链接已失效");
        }
        if (shareLink.getExpireTime() != null && shareLink.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("分享链接已过期");
        }
        if (shareLink.getMaxUses() > 0 && shareLink.getUsedCount() >= shareLink.getMaxUses()) {
            throw new BusinessException("分享链接已达到使用上限");
        }
        
        // 检查是否已有权限
        LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(DocumentPermission::getDocumentId, shareLink.getDocumentId())
                   .eq(DocumentPermission::getUserId, userId);
        if (permissionMapper.selectCount(permWrapper) > 0) {
            // 已有权限，不需要再添加
            return;
        }
        
        // 添加权限
        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(shareLink.getDocumentId());
        permission.setUserId(userId);
        permission.setPermissionType(shareLink.getPermissionType());
        permissionMapper.insert(permission);
        
        // 更新使用次数
        shareLink.setUsedCount(shareLink.getUsedCount() + 1);
        shareLinkMapper.updateById(shareLink);
        
        log.info("User {} joined document {} via share link", userId, shareLink.getDocumentId());
    }
    
    @Override
    public List<ShareLinkDTO> getShareLinks(Long documentId, Long userId) {
        // 验证权限
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!document.getCreatorId().equals(userId)) {
            throw new BusinessException("无权查看分享链接");
        }
        
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getDocumentId, documentId)
               .orderByDesc(ShareLink::getCreateTime);
        
        List<ShareLink> links = shareLinkMapper.selectList(wrapper);
        return links.stream()
                .map(link -> convertToDTO(link, document.getTitle()))
                .collect(Collectors.toList());
    }
    
    @Override
    public void disableShareLink(Long linkId, Long userId) {
        ShareLink shareLink = shareLinkMapper.selectById(linkId);
        if (shareLink == null) {
            throw new BusinessException("分享链接不存在");
        }
        if (!shareLink.getCreatorId().equals(userId)) {
            throw new BusinessException("无权操作此链接");
        }
        
        shareLink.setStatus(0);
        shareLinkMapper.updateById(shareLink);
        
        log.info("Disabled share link {} by user {}", linkId, userId);
    }
    
    @Override
    public ShareLinkDTO getShareLinkByToken(String token) {
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null) {
            return null;
        }
        
        Document document = documentMapper.selectById(shareLink.getDocumentId());
        String title = document != null ? document.getTitle() : "";
        return convertToDTO(shareLink, title);
    }
    
    private ShareLinkDTO convertToDTO(ShareLink shareLink, String documentTitle) {
        ShareLinkDTO dto = new ShareLinkDTO();
        dto.setId(String.valueOf(shareLink.getId()));
        dto.setDocumentId(String.valueOf(shareLink.getDocumentId()));
        dto.setDocumentTitle(documentTitle);
        dto.setToken(shareLink.getToken());
        dto.setShareUrl("/share/" + shareLink.getToken());
        dto.setPermissionType(shareLink.getPermissionType());
        dto.setExpireTime(shareLink.getExpireTime());
        dto.setMaxUses(shareLink.getMaxUses());
        dto.setUsedCount(shareLink.getUsedCount());
        dto.setStatus(shareLink.getStatus());
        dto.setCreateTime(shareLink.getCreateTime());
        return dto;
    }
}
