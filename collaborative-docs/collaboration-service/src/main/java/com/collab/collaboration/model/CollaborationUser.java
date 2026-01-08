package com.collab.collaboration.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 协作用户会话信息
 */
@Data
public class CollaborationUser {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * WebSocket会话
     */
    private WebSocketSession session;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 用户站点ID（用于CRDT）
     */
    private String siteId;
    
    /**
     * 加入时间
     */
    private long joinTime;

    public CollaborationUser(Long userId, String username, WebSocketSession session, Long documentId) {
        this.userId = userId;
        this.username = username;
        this.session = session;
        this.documentId = documentId;
        this.siteId = "site_" + userId + "_" + System.currentTimeMillis();
        this.joinTime = System.currentTimeMillis();
    }
}
