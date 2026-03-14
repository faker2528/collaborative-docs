package com.collab.document.controller;

import com.collab.common.dto.CreateShareLinkRequest;
import com.collab.common.dto.ShareLinkDTO;
import com.collab.common.result.Result;
import com.collab.document.service.ShareLinkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分享链接控制器
 */
@Slf4j
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareLinkController {
    
    private final ShareLinkService shareLinkService;
    
    /**
     * 创建分享链接
     */
    @PostMapping("/link")
    public Result<ShareLinkDTO> createShareLink(@RequestBody CreateShareLinkRequest request,
                                                 @RequestHeader("X-User-Id") Long userId) {
        ShareLinkDTO link = shareLinkService.createShareLink(request, userId);
        return Result.success("分享链接创建成功", link);
    }
    
    /**
     * 通过分享链接加入文档
     */
    @PostMapping("/join/{token}")
    public Result<Void> joinByShareLink(@PathVariable("token") String token,
                                        @RequestHeader("X-User-Id") Long userId) {
        log.info("用户:{}, 通过分享链接加入文档: {}", userId, token);
        shareLinkService.joinByShareLink(token, userId);
        return Result.success("已成功加入文档协作", null);
    }
    
    /**
     * 获取文档的分享链接列表
     */
    @GetMapping("/links/{documentId}")
    public Result<List<ShareLinkDTO>> getShareLinks(@PathVariable("documentId") String documentId,
                                                    @RequestHeader("X-User-Id") Long userId) {
        List<ShareLinkDTO> links = shareLinkService.getShareLinks(Long.parseLong(documentId), userId);
        return Result.success(links);
    }
    
    /**
     * 禁用分享链接
     */
    @DeleteMapping("/link/{linkId}")
    public Result<Void> disableShareLink(@PathVariable("linkId") String linkId,
                                         @RequestHeader("X-User-Id") Long userId) {
        shareLinkService.disableShareLink(Long.parseLong(linkId), userId);
        return Result.success("分享链接已禁用", null);
    }
    
    /**
     * 根据token获取链接信息（公开接口，用于预览）
     */
    @GetMapping("/info/{token}")
    public Result<ShareLinkDTO> getShareLinkInfo(@PathVariable("token") String token, HttpServletResponse response) {
        // 禁用缓存
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        ShareLinkDTO link = shareLinkService.getShareLinkByToken(token);
        if (link == null) {
            return Result.error("分享链接不存在");
        }
        return Result.success(link);
    }
    
    /**
     * 根据token获取文档基本信息（用于预览，无需登录）
     */
    @GetMapping("/preview/{token}")
    public Result<Map<String, Object>> getDocumentPreview(@PathVariable("token") String token, HttpServletResponse response) {
        // 禁用缓存
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        Map<String, Object> result = shareLinkService.getDocumentPreviewByToken(token);
        if (result == null) {
            return Result.error("文档不存在或链接已失效");
        }
        return Result.success(result);
    }
    
    /**
     * 验证密码（用于密码验证类型的链接）
     */
    @PostMapping("/verify-password/{token}")
    public Result<Boolean> verifyPassword(@PathVariable("token") String token, @RequestBody Map<String, String> requestBody) {
        String password = requestBody.get("password");
        boolean isValid = shareLinkService.verifyPassword(token, password);
        if (isValid) {
            return Result.success("密码验证成功", true);
        } else {
            return Result.error("密码错误");
        }
    }
}
