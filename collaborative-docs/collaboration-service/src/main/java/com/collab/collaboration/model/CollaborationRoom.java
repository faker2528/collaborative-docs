package com.collab.collaboration.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.collab.common.crdt.CrdtDocument;
import com.collab.common.crdt.CrdtOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作房间
 * 一个文档对应一个协作房间
 * 
 * 使用 CRDT 字符级别操作保证一致性
 */
@Data
@Slf4j
public class CollaborationRoom {
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * CRDT文档
     */
    private CrdtDocument crdtDocument;
    
    /**
     * 房间内的用户（sessionId -> CollaborationUser）
     */
    private Map<String, CollaborationUser> users;
    
    /**
     * 房间创建时间
     */
    private long createTime;
    
    /**
     * 最后活动时间
     */
    private volatile long lastActiveTime;
    
    /**
     * 房间创建者ID（用于保存时的权限验证）
     */
    private Long creatorUserId;
    
    /**
     * 文档是否有未保存的编辑
     */
    private volatile boolean dirty = false;

    public CollaborationRoom(Long documentId) {
        this.documentId = documentId;
        this.crdtDocument = new CrdtDocument(documentId, "server");
        this.users = new ConcurrentHashMap<>();
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
    }

    /**
     * 添加用户到房间
     */
    public void addUser(CollaborationUser user) {
        String oldSessionId = null;
        for (Map.Entry<String, CollaborationUser> entry : users.entrySet()) {
            if (entry.getValue().getUserId().equals(user.getUserId())) {
                oldSessionId = entry.getKey();
                break;
            }
        }
        
        if (oldSessionId != null) {
            users.remove(oldSessionId);
            log.info("Removing old connection for user {} in room {}", user.getUsername(), documentId);
        }
        
        users.put(user.getSession().getId(), user);
        lastActiveTime = System.currentTimeMillis();
        log.info("User {} joined room {}, current users: {}", user.getUsername(), documentId, users.size());
    }

    /**
     * 从房间移除用户
     */
    public void removeUser(String sessionId) {
        CollaborationUser removed = users.remove(sessionId);
        if (removed != null) {
            lastActiveTime = System.currentTimeMillis();
            log.info("User {} left room {}, current users: {}", removed.getUsername(), documentId, users.size());
        }
    }

    /**
     * 获取房间内用户数量
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * 检查房间是否为空
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * 更新最后活动时间
     */
    public void touch() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 获取文档纯文本内容
     */
    public String getContent() {
        return crdtDocument.getText();
    }
    
    /**
     * 获取文档内容（Quill Delta JSON 格式）
     */
    public String getDeltaContent() {
        String text = crdtDocument.getText();
        JSONObject delta = new JSONObject();
        JSONArray ops = new JSONArray();
        if (text != null && !text.isEmpty()) {
            JSONObject insertOp = new JSONObject();
            insertOp.put("insert", text);
            ops.add(insertOp);
        }
        delta.put("ops", ops);
        return delta.toJSONString();
    }
    
    /**
     * 应用 Quill Delta 操作到 CRDT 文档
     * 解析 Delta 并转换为字符级别的 CRDT 操作
     * 
     * @param deltaJson Delta JSON 字符串
     * @param siteId 站点ID
     * @return 生成的 CRDT 操作列表
     */
    public synchronized List<CrdtOperation> applyDelta(String deltaJson, String siteId) {
        List<CrdtOperation> operations = new ArrayList<>();
        
        if (deltaJson == null || deltaJson.isEmpty()) {
            return operations;
        }
        
        try {
            JSONObject delta = JSON.parseObject(deltaJson);
            JSONArray deltaOps = delta.getJSONArray("ops");
            if (deltaOps == null) {
                return operations;
            }
            
            int cursor = 0; // 当前光标位置
            
            for (int i = 0; i < deltaOps.size(); i++) {
                JSONObject op = deltaOps.getJSONObject(i);
                
                if (op.containsKey("retain")) {
                    // 保留字符，移动光标
                    cursor += op.getIntValue("retain");
                    
                } else if (op.containsKey("insert")) {
                    // 插入文本
                    Object insertValue = op.get("insert");
                    if (insertValue instanceof String) {
                        String insertText = (String) insertValue;
                        // 逐字符插入到 CRDT
                        for (int j = 0; j < insertText.length(); j++) {
                            String ch = String.valueOf(insertText.charAt(j));
                            CrdtOperation crdtOp = crdtDocument.insertAt(cursor + j, ch);
                            if (crdtOp != null) {
                                operations.add(crdtOp);
                            }
                        }
                        cursor += insertText.length();
                    }
                    
                } else if (op.containsKey("delete")) {
                    // 删除字符
                    int deleteCount = op.getIntValue("delete");
                    // 从后向前删除，避免索引偏移
                    for (int j = 0; j < deleteCount; j++) {
                        CrdtOperation crdtOp = crdtDocument.deleteAt(cursor);
                        if (crdtOp != null) {
                            operations.add(crdtOp);
                        }
                    }
                }
            }
            
            touch();
            markDirty(); // 标记有未保存的编辑
            log.debug("Applied delta to room {}, generated {} CRDT operations", documentId, operations.size());
            
        } catch (Exception e) {
            log.error("Failed to apply delta to room {}: {}", documentId, e.getMessage());
        }
        
        return operations;
    }
    
    /**
     * 应用远程 CRDT 操作
     */
    public void applyRemoteOperation(CrdtOperation operation) {
        if (operation != null) {
            crdtDocument.applyRemoteOperation(operation);
            touch();
            markDirty();
        }
    }
    
    /**
     * 标记文档有未保存的编辑
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    /**
     * 清除脏标记
     */
    public void clearDirty() {
        this.dirty = false;
    }
    
    /**
     * 检查是否有未保存的编辑
     */
    public boolean isDirty() {
        return this.dirty;
    }
}
