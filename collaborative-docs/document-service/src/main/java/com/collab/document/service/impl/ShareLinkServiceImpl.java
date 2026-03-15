package com.collab.document.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.collab.common.constant.RedisConstant;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareLinkDTO createShareLink(CreateShareLinkRequest request, String userId) {
        log.info("开始创建分享链接，userId: {}, documentId: {}", userId, request.getDocumentId());
            
        // 参数校验
        if (!StringUtils.hasText(request.getDocumentId())) {
            throw new BusinessException("文档 ID 不能为空");
        }
            
        // 解析 documentId
        String documentId;
        try {
            documentId = request.getDocumentId();
        } catch (NumberFormatException e) {
            log.error("【创建分享链接】文档 ID 格式错误：{}", request.getDocumentId());
            throw new BusinessException("文档 ID 格式错误");
        }
            
        // 验证文档存在
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.warn("【创建分享链接】文档不存在：documentId: {}", documentId);
            throw new BusinessException("文档不存在");
        }
        
        // 检查用户是否有权限访问该文档
        boolean isCreator = document.getCreatorId().equals(userId);
        if (!isCreator) {
            LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(DocumentPermission::getDocumentId, documentId)
                       .eq(DocumentPermission::getUserId, userId);
            Long count = permissionMapper.selectCount(permWrapper);
            if (count == null || count == 0) {
                log.warn("【创建分享链接】用户无权操作此文档：userId: {}, documentId: {}", userId, documentId);
                throw new BusinessException("无权操作此文档");
            }
        }
        
        // 非创建者只能生成只读链接
        Integer permissionType = request.getPermissionType();
        if (permissionType == null) {
            permissionType = 1; // 默认为只读
        }
            
        if (!isCreator) {
            permissionType = 1; // 强制只读
            log.info("非创建者 {} 生成分享链接，强制设置为只读权限", userId);
        }
            
        // 验证权限类型
        if (permissionType != 1 && permissionType != 2) {
            throw new BusinessException("权限类型必须为 1(只读) 或 2(可编辑)");
        }
            
        // 生成唯一 token（使用时间戳 + 随机字符串）
        String token = generateSecureToken();
            
        // 创建分享链接
        ShareLink shareLink = new ShareLink();
        shareLink.setDocumentId(documentId);
        shareLink.setCreatorId(userId);
        shareLink.setToken(token);
        shareLink.setPermissionType(permissionType);
            
        // 设置验证类型和相关信息
        Integer verificationType = request.getVerificationType();
        if (verificationType == null) {
            verificationType = 0; // 默认免验证
        }
        shareLink.setVerificationType(verificationType);
            
        // 根据验证类型设置相应信息
        if (verificationType == 1) {
            // 密码验证：加密存储密码
            if (!StringUtils.hasText(request.getPassword())) {
                throw new BusinessException("密码验证类型必须设置密码");
            }
            shareLink.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("【创建分享链接】已加密存储访问密码");
        } else if (verificationType == 2) {
            // 邮箱验证：验证邮箱格式
            if (!StringUtils.hasText(request.getEmail())) {
                throw new BusinessException("邮箱验证类型必须设置邮箱");
            }
            if (!isValidEmail(request.getEmail())) {
                throw new BusinessException("邮箱格式不正确");
            }
            shareLink.setEmail(request.getEmail().trim().toLowerCase());
            log.info("【创建分享链接】设置指定邮箱：{}", request.getEmail());
        }
            
        // 设置过期时间
        Integer validDays = request.getValidDays();
        if (validDays != null && validDays > 0) {
            LocalDateTime expireTime = LocalDateTime.now().plusDays(validDays);
            shareLink.setExpireTime(expireTime);
            log.info("【创建分享链接】设置链接过期时间：{}", expireTime);
        }
            
        // 设置使用次数限制
        Integer maxUses = request.getMaxUses();
        if (maxUses == null) {
            maxUses = 0; // 默认无限制
        }
        if (maxUses < 0) {
            throw new BusinessException("最大使用次数不能为负数");
        }
        shareLink.setMaxUses(maxUses);
            
        shareLink.setUsedCount(0);
        shareLink.setStatus(1); // 正常状态
            
        // 保存到数据库
        int insertResult = shareLinkMapper.insert(shareLink);
        if (insertResult != 1) {
            log.error("保存分享链接失败：{}", shareLink);
            throw new BusinessException("保存分享链接失败");
        }
            
        log.info("成功创建分享链接：linkId: {}, token: {}, documentId: {}, userId: {}, permissionType: {}", 
                shareLink.getId(), token, documentId, userId, permissionType);
            
        return convertToDTO(shareLink, document.getTitle());
    }
    
    /**
     * 通过分享链接加入文档协作
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinByShareLink(String token, String userId) {
        log.info("用户尝试通过分享链接加入文档：userId: {}, token: {}", userId, token);
        
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("分享令牌不能为空");
        }
        
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        
        // 查找链接
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null) {
            log.warn("【加入文档协作】分享链接不存在：token: {}", token);
            throw new BusinessException("分享链接不存在");
        }
        
        // 校验链接状态
        validateShareLink(shareLink);
        
        // 检查验证类型
        Integer verificationType = shareLink.getVerificationType();
        if (verificationType != null) {
            if (verificationType == 1) {
                // 密码验证：检查是否已通过密码验证
                String verifyKey = RedisConstant.SHARE_LINK_VERIFIED_PREFIX + token;
                String verified = redisTemplate.opsForValue().get(verifyKey);
                if (!"1".equals(verified)) {
                    log.warn("【加入文档协作】密码验证未通过：token: {}", token);
                    throw new BusinessException("请先通过密码验证");
                }
                // 验证成功后删除标记，避免重复使用
                redisTemplate.delete(verifyKey);
                log.info("【加入文档协作】密码验证通过：userId: {}, documentId: {}", userId, shareLink.getDocumentId());
                
            } else if (verificationType == 2) {
                // 邮箱验证：暂时不实现具体逻辑，留给后续扩展
                log.info("【加入文档协作】邮箱验证类型：token: {}, requiredEmail: {}", token, shareLink.getEmail());
                // TODO: 未来可以通过 RPC 调用用户服务验证邮箱
            }
        }
        
        // 检查是否已有权限（幂等性处理）
        LambdaQueryWrapper<DocumentPermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(DocumentPermission::getDocumentId, shareLink.getDocumentId())
                   .eq(DocumentPermission::getUserId, userId);
        Long existingCount = permissionMapper.selectCount(permWrapper);
        
        if (existingCount != null && existingCount > 0) {
            log.info("【加入文档协作】用户已有权限，跳过添加：userId: {}, documentId: {}", userId, shareLink.getDocumentId());
            return;
        }
        
        // 添加文档权限
        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(shareLink.getDocumentId());
        permission.setUserId(userId);
        permission.setPermissionType(shareLink.getPermissionType());
        
        int permInsertResult = permissionMapper.insert(permission);
        if (permInsertResult != 1) {
            log.error("【加入文档协作】添加文档权限失败：userId: {}, documentId: {}", userId, shareLink.getDocumentId());
            throw new BusinessException("添加权限失败");
        }
        log.info("【加入文档协作】添加文档权限成功：userId: {}, documentId: {}, permissionType: {}",
                userId, shareLink.getDocumentId(), shareLink.getPermissionType());
        
        // 更新使用次数（使用乐观锁 CAS 操作）
        boolean updateSuccess = incrementUsedCountWithOptimisticLock(shareLink.getId(), shareLink.getUsedCount());
        if (!updateSuccess) {
            log.warn("【加入文档协作】更新使用次数失败（可能并发冲突），重试中... token: {}", token);
            // 重新查询并更新
            ShareLink currentLink = shareLinkMapper.selectById(shareLink.getId());
            if (currentLink != null) {
                currentLink.setUsedCount(currentLink.getUsedCount() + 1);
                shareLinkMapper.updateById(currentLink);
                log.info("【加入文档协作】重试更新使用次数成功：token: {}, usedCount: {}", token, currentLink.getUsedCount());
            }
        } else {
            log.info("【加入文档协作】更新使用次数成功：token: {}, newUsedCount: {}", token, shareLink.getUsedCount() + 1);
        }
        
        log.info("【加入文档协作】用户成功加入文档协作：userId: {}, documentId: {}, token: {}",
                userId, shareLink.getDocumentId(), token);
    }
    
    @Override
    public List<ShareLinkDTO> getShareLinks(String documentId, String userId) {
        log.info("获取文档分享链接列表：documentId: {}, userId: {}", documentId, userId);
        
        if (documentId == null) {
            throw new BusinessException("文档 ID 不能为空");
        }
        
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        
        // 验证文档存在
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.warn("【获取文档分享链接列表】文档不存在：documentId: {}", documentId);
            throw new BusinessException("文档不存在");
        }
        
        // 只有文档创建者才能查看分享链接
        if (!document.getCreatorId().equals(userId)) {
            log.warn("【获取文档分享链接列表】用户无权查看分享链接：userId: {}, documentId: {}", userId, documentId);
            throw new BusinessException("无权查看分享链接");
        }
        
        // 查询分享链接列表（按创建时间倒序）
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getDocumentId, documentId)
               .orderByDesc(ShareLink::getCreateTime);
        
        List<ShareLink> links = shareLinkMapper.selectList(wrapper);
        log.info("【获取文档分享链接列表】查询到 {} 条分享链接记录", links.size());
        
        return links.stream()
                .map(link -> convertToDTO(link, document.getTitle()))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableShareLink(String linkId, String userId) {
        log.info("禁用分享链接：linkId: {}, userId: {}", linkId, userId);
        
        if (linkId == null) {
            throw new BusinessException("链接 ID 不能为空");
        }
        
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        
        ShareLink shareLink = shareLinkMapper.selectById(linkId);
        if (shareLink == null) {
            log.warn("【禁用分享链接】分享链接不存在：linkId: {}", linkId);
            throw new BusinessException("分享链接不存在");
        }
        
        // 只有创建者才能禁用链接
        if (!shareLink.getCreatorId().equals(userId)) {
            log.warn("【禁用分享链接】用户无权操作此链接：userId: {}, linkId: {}", userId, linkId);
            throw new BusinessException("无权操作此链接");
        }
        
        // 更新状态为禁用
        shareLink.setStatus(0);
        int updateResult = shareLinkMapper.updateById(shareLink);
        
        if (updateResult != 1) {
            log.error("【禁用分享链接】禁用分享链接失败：linkId: {}", linkId);
            throw new BusinessException("禁用分享链接失败");
        }
        
        log.info("【禁用分享链接】成功禁用分享链接：linkId: {}, userId: {}", linkId, userId);
    }
    
    @Override
    public ShareLinkDTO getShareLinkByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null) {
            log.debug("【获取分享链接】分享链接不存在：token: {}", token);
            return null;
        }
        
        Document document = documentMapper.selectById(shareLink.getDocumentId());
        String title = document != null ? document.getTitle() : "";
        
        return convertToDTO(shareLink, title);
    }
    
    @Override
    public Map<String, Object> getDocumentPreviewByToken(String token) {
        log.debug("【获取文档预览信息】获取文档预览信息：token: {}", token);
        
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        // 查询分享链接
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null || shareLink.getStatus() != 1) {
            log.debug("【获取文档预览信息】分享链接不存在或已失效：token: {}", token);
            return null;
        }
        
        // 校验链接状态（过期、使用次数等）
        try {
            validateShareLink(shareLink);
        } catch (BusinessException e) {
            log.warn("【获取文档预览信息】分享链接校验失败：token: {}, message: {}", token, e.getMessage());
            return null;
        }
        
        // 获取文档信息
        Document document = documentMapper.selectById(shareLink.getDocumentId());
        if (document == null) {
            log.warn("【获取文档预览信息】文档不存在：documentId: {}", shareLink.getDocumentId());
            return null;
        }
        
        // 构建预览数据
        Map<String, Object> previewData = new HashMap<>(8);
        previewData.put("documentId", document.getId().toString());
        previewData.put("title", document.getTitle());
        previewData.put("permissionType", shareLink.getPermissionType());
        previewData.put("creatorId", document.getCreatorId());
        previewData.put("verificationType", shareLink.getVerificationType());
        previewData.put("requiresPassword", 
                shareLink.getVerificationType() != null && shareLink.getVerificationType() == 1);
        
        // 处理文档内容（转换为 HTML）
        String content = document.getContent();
        if (content != null && !content.isEmpty()) {
            try {
                // 尝试解析为 JSON，如果是 Delta 格式则转换为 HTML
                if (content.trim().startsWith("{")) {
                    JSONObject deltaJson = JSON.parseObject(content);
                    if (deltaJson.containsKey("ops")) {
                        // 是 Quill Delta 格式，转换为 HTML
                        content = convertDeltaToHtml(deltaJson);
                    }
                }
                // 如果已经是 HTML 或其他格式，直接使用
            } catch (Exception e) {
                log.warn("【获取文档预览信息】解析文档内容失败，使用原始内容：{}", e.getMessage());
                // 使用原始内容
            }
        } else {
            content = "<p>文档为空</p>";
        }
        
        previewData.put("content", content);
        previewData.put("createTime", document.getCreateTime());
        
        log.debug("【获取文档预览信息】文档预览信息获取成功：token: {}, documentId: {}", token, document.getId());
        
        return previewData;
    }
    
    @Override
    public boolean verifyPassword(String token, String password) {
        log.info("【验证分享链接密码】验证分享链接密码：token: {}", token);
        
        if (!StringUtils.hasText(token)) {
            return false;
        }
        
        if (!StringUtils.hasText(password)) {
            return false;
        }
        
        // 查询分享链接
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink shareLink = shareLinkMapper.selectOne(wrapper);
        
        if (shareLink == null || shareLink.getStatus() != 1) {
            log.debug("【验证分享链接密码】分享链接不存在或已失效：token: {}", token);
            return false;
        }
        
        // 检查验证类型是否为密码验证
        Integer verificationType = shareLink.getVerificationType();
        if (verificationType == null || verificationType != 1) {
            log.warn("【验证分享链接密码】不是密码验证类型：token: {}, verificationType: {}", token, verificationType);
            return false;
        }
        
        // 检查密码是否匹配（使用 BCrypt 加密比较）
        String encodedPassword = shareLink.getPassword();
        if (!StringUtils.hasText(encodedPassword)) {
            log.warn("【验证分享链接密码】分享链接未设置密码：token: {}", token);
            return false;
        }
        
        boolean isValid = passwordEncoder.matches(password, encodedPassword);
        
        if (isValid) {
            // 密码验证成功，在 Redis 中存储验证标记，有效期 30 分钟
            String key = RedisConstant.SHARE_LINK_VERIFIED_PREFIX + token;
            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.MINUTES);
            log.info("【验证分享链接密码】密码验证成功：token: {}, validFor: 30 minutes", token);
        } else {
            log.warn("【验证分享链接密码】密码验证失败：token: {}", token);
        }
        
        return isValid;
    }
    
    /**
     * 将 Quill Delta JSON 转换为 HTML
     */
    private String convertDeltaToHtml(JSONObject deltaJson) {
        if (deltaJson == null) {
            return "<p>文档为空</p>";
        }
        
        StringBuilder html = new StringBuilder();
        JSONArray ops = deltaJson.getJSONArray("ops");
        
        if (ops == null || ops.isEmpty()) {
            return "<p>文档为空</p>";
        }
        
        for (int i = 0; i < ops.size(); i++) {
            JSONObject op = ops.getJSONObject(i);
            if (op != null) {
                String insert = op.getString("insert");
                if (insert != null) {
                    // 简单转换：换行符转<p>标签
                    String[] lines = insert.split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            html.append("<p>").append(line).append("</p>");
                        }
                    }
                }
            }
        }
        
        return html.length() > 0 ? html.toString() : "<p>文档为空</p>";
    }
    
    /**
     * 将 ShareLink 实体转换为 DTO
     */
    private ShareLinkDTO convertToDTO(ShareLink shareLink, String documentTitle) {
        if (shareLink == null) {
            return null;
        }
        
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
        
        // 添加验证相关字段
        dto.setVerificationType(shareLink.getVerificationType());
        dto.setEmail(shareLink.getEmail());
        
        return dto;
    }
    
    /**
     * 生成安全的 Token（使用时间戳 + 随机字符串）
     */
    private String generateSecureToken() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomStr = RandomUtil.randomString(16);
        return timestamp + randomStr;
    }
    
    /**
     * 校验邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        // 简单的邮箱格式校验正则
        String regex = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        return email.matches(regex);
    }
    
    /**
     * 校验分享链接状态（过期、使用次数、状态等）
     */
    private void validateShareLink(ShareLink shareLink) {
        if (shareLink == null) {
            throw new BusinessException("分享链接不存在");
        }
        
        // 检查状态
        if (shareLink.getStatus() != 1) {
            log.warn("分享链接已禁用：linkId: {}", shareLink.getId());
            throw new BusinessException("分享链接已失效");
        }
        
        // 检查过期时间
        if (shareLink.getExpireTime() != null && 
            shareLink.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("分享链接已过期：linkId: {}, expireTime: {}", 
                    shareLink.getId(), shareLink.getExpireTime());
            throw new BusinessException("分享链接已过期");
        }
        
        // 检查使用次数限制
        if (shareLink.getMaxUses() > 0 && 
            shareLink.getUsedCount() >= shareLink.getMaxUses()) {
            log.warn("分享链接已达到使用上限：linkId: {}, usedCount: {}, maxUses: {}", 
                    shareLink.getId(), shareLink.getUsedCount(), shareLink.getMaxUses());
            throw new BusinessException("分享链接已达到使用上限");
        }
    }
    
    /**
     * 使用乐观锁更新使用次数（防止并发问题）
     */
    private boolean incrementUsedCountWithOptimisticLock(String linkId, Integer currentUsedCount) {
        try {
            LambdaUpdateWrapper<ShareLink> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ShareLink::getId, linkId)
                        .eq(ShareLink::getUsedCount, currentUsedCount)
                        .setSql("used_count = used_count + 1");
            
            int updateResult = shareLinkMapper.update(null, updateWrapper);
            return updateResult > 0;
        } catch (Exception e) {
            log.error("乐观锁更新使用次数失败：linkId: {}", linkId, e);
            return false;
        }
    }
}
