package com.collab.common.constant;

/**
 * Redis Key常量
 */
public class RedisConstant {
    
    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "collab:token:";
    
    /**
     * 用户信息前缀
     */
    public static final String USER_PREFIX = "collab:user:";
    
    /**
     * 文档前缀
     */
    public static final String DOCUMENT_PREFIX = "collab:doc:";
    
    /**
     * 文档锁前缀
     */
    public static final String DOCUMENT_LOCK_PREFIX = "collab:doc:lock:";
    
    /**
     * 协作房间前缀
     */
    public static final String COLLABORATION_ROOM_PREFIX = "collab:room:";
    
    /**
     * 协作房间用户前缀
     */
    public static final String COLLABORATION_ROOM_USERS_PREFIX = "collab:room:users:";
    
    /**
     * CRDT文档状态前缀
     */
    public static final String CRDT_DOC_PREFIX = "collab:crdt:";
    
    /**
     * Token过期时间（秒）- 24小时
     */
    public static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60;
    
    /**
     * 文档缓存过期时间（秒）- 1小时
     */
    public static final long DOCUMENT_EXPIRE_TIME = 60 * 60;
}
