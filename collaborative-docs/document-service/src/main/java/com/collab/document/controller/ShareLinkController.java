package com.collab.document.controller;

import com.collab.common.dto.CreateShareLinkRequest;
import com.collab.common.dto.ShareLinkDTO;
import com.collab.common.result.Result;
import com.collab.document.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分享链接控制器
 */
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
    public Result<Void> joinByShareLink(@PathVariable String token,
                                        @RequestHeader("X-User-Id") Long userId) {
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
    public Result<ShareLinkDTO> getShareLinkInfo(@PathVariable String token) {
        ShareLinkDTO link = shareLinkService.getShareLinkByToken(token);
        if (link == null) {
            return Result.error("分享链接不存在");
        }
        return Result.success(link);
    }
}
