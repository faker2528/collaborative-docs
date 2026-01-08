package com.collab.document.service;

import com.collab.common.dto.CreateShareLinkRequest;
import com.collab.common.dto.ShareLinkDTO;

import java.util.List;

/**
 * 分享链接服务接口
 */
public interface ShareLinkService {
    
    /**
     * 创建分享链接
     */
    ShareLinkDTO createShareLink(CreateShareLinkRequest request, Long userId);
    
    /**
     * 通过token验证并加入文档
     */
    void joinByShareLink(String token, Long userId);
    
    /**
     * 获取文档的分享链接列表
     */
    List<ShareLinkDTO> getShareLinks(Long documentId, Long userId);
    
    /**
     * 禁用分享链接
     */
    void disableShareLink(Long linkId, Long userId);
    
    /**
     * 根据token获取链接信息
     */
    ShareLinkDTO getShareLinkByToken(String token);
}
