package com.collab.collaboration.model;

import com.collab.common.crdt.CrdtOperation;
import lombok.Data;

import java.util.List;

/**
 * WebSocket消息
 */
@Data
public class WebSocketMessage {
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 发送者用户ID
     */
    private Long userId;
    
    /**
     * 发送者用户名
     */
    private String username;
    
    /**
     * 发送者站点ID
     */
    private String siteId;
    
    /**
     * CRDT操作（单个操作）
     */
    private CrdtOperation operation;
    
    /**
     * CRDT操作列表（批量操作）
     */
    private List<CrdtOperation> operations;
    
    /**
     * 文档内容（同步时使用）
     */
    private String content;
    
    /**
     * 在线用户列表
     */
    private List<OnlineUser> onlineUsers;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 客户端发送
        JOIN,           // 加入房间
        LEAVE,          // 离开房间
        OPERATION,      // 单个操作
        OPERATIONS,     // 批量操作
        SYNC_REQUEST,   // 请求同步文档
        CURSOR_MOVE,    // 光标移动
        
        // 服务端发送
        JOINED,         // 加入成功
        USER_JOINED,    // 其他用户加入
        USER_LEFT,      // 其他用户离开
        SYNC_RESPONSE,  // 同步响应
        REMOTE_OPERATION,  // 远程操作
        REMOTE_OPERATIONS, // 远程批量操作
        ONLINE_USERS,   // 在线用户列表
        ERROR           // 错误
    }

    /**
     * 在线用户信息
     */
    @Data
    public static class OnlineUser {
        private Long userId;
        private String username;
        private String siteId;

        public OnlineUser(Long userId, String username, String siteId) {
            this.userId = userId;
            this.username = username;
            this.siteId = siteId;
        }
    }
}
