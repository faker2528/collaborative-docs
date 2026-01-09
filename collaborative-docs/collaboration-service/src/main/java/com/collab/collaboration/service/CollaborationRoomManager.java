package com.collab.collaboration.service;

import com.collab.collaboration.model.CollaborationRoom;
import com.collab.collaboration.model.CollaborationUser;
import com.collab.common.crdt.CrdtOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作房间管理器
 * 管理所有文档的协作房间
 */
@Slf4j
@Service
public class CollaborationRoomManager {

    /**
     * 所有协作房间（documentId -> Room）
     */
    private final Map<Long, CollaborationRoom> rooms = new ConcurrentHashMap<>();

    /**
     * 获取或创建房间
     */
    public CollaborationRoom getOrCreateRoom(Long documentId) {
        return rooms.computeIfAbsent(documentId, id -> {
            log.info("【Creating new collaboration room for document】: {}", id);
            return new CollaborationRoom(id);
        });
    }

    /**
     * 获取房间
     */
    public CollaborationRoom getRoom(Long documentId) {
        return rooms.get(documentId);
    }

    /**
     * 用户加入房间
     */
    public void joinRoom(Long documentId, CollaborationUser user) {
        CollaborationRoom room = getOrCreateRoom(documentId);
        room.addUser(user);
    }

    /**
     * 用户离开房间
     */
    public void leaveRoom(Long documentId, String sessionId) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            room.removeUser(sessionId);
            
            if (room.isEmpty()) {
                log.info("Room {} is empty, scheduling cleanup", documentId);
            }
        }
    }

    /**
     * 应用 Delta 操作并返回生成的 CRDT 操作
     * 
     * @param documentId 文档ID
     * @param deltaJson Delta JSON 字符串
     * @param siteId 站点ID
     * @return 生成的 CRDT 操作列表
     */
    public List<CrdtOperation> applyDelta(Long documentId, String deltaJson, String siteId) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            List<CrdtOperation> ops = room.applyDelta(deltaJson, siteId);
            log.debug("【Applied delta to document {}】, generated {} CRDT ops", documentId, ops.size());
            return ops;
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * 应用远程 CRDT 操作
     */
    public void applyRemoteOperation(Long documentId, CrdtOperation operation) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            room.applyRemoteOperation(operation);
        }
    }

    /**
     * 获取房间内的所有用户（返回副本，避免并发问题）
     */
    public Collection<CollaborationUser> getRoomUsers(Long documentId) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            // 返回副本而不是 live view，避免并发修改问题
            return new java.util.ArrayList<>(room.getUsers().values());
        }
        return java.util.Collections.emptyList();
    }

    /**
     * 获取文档内容 (Delta JSON)
     */
    public String getDocumentContent(Long documentId) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            return room.getDeltaContent();
        }
        return "";
    }
    
    /**
     * 获取文档纯文本
     */
    public String getDocumentText(Long documentId) {
        CollaborationRoom room = rooms.get(documentId);
        if (room != null) {
            return room.getContent();
        }
        return "";
    }

    /**
     * 初始化文档内容（只在房间为空时初始化，避免重复加载）
     */
    public synchronized void initDocumentContent(Long documentId, String content) {
        CollaborationRoom room = getOrCreateRoom(documentId);
        
        // 检查房间是否已经初始化过内容（避免重复加载）
        if (room.isContentInitialized()) {
            log.debug("Room {} content already initialized, skip", documentId);
            return;
        }
        
        // 将内容初始化到 CRDT 文档
        if (content != null && !content.isEmpty()) {
            // 尝试解析为 Delta
            String textContent = content;
            try {
                com.alibaba.fastjson2.JSONObject delta = com.alibaba.fastjson2.JSON.parseObject(content);
                if (delta != null && delta.containsKey("ops")) {
                    // 提取纯文本
                    StringBuilder sb = new StringBuilder();
                    com.alibaba.fastjson2.JSONArray ops = delta.getJSONArray("ops");
                    for (int i = 0; i < ops.size(); i++) {
                        Object insert = ops.getJSONObject(i).get("insert");
                        if (insert instanceof String) {
                            sb.append(insert);
                        }
                    }
                    textContent = sb.toString();
                }
            } catch (Exception e) {
                // 不是 JSON，使用原始内容
            }
            room.getCrdtDocument().initFromText(textContent);
        }
        
        // 标记已初始化
        room.setContentInitialized(true);
        log.info("Initialized document {} with content length: {}", documentId, content != null ? content.length() : 0);
    }

    /**
     * 清理空闲房间
     */
    public void cleanupIdleRooms(long idleThreshold) {
        long now = System.currentTimeMillis();
        rooms.entrySet().removeIf(entry -> {
            CollaborationRoom room = entry.getValue();
            if (room.isEmpty() && (now - room.getLastActiveTime() > idleThreshold)) {
                log.info("Removing idle room: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 获取活跃房间数量
     */
    public int getActiveRoomCount() {
        return rooms.size();
    }
}
