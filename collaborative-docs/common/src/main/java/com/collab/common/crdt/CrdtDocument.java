package com.collab.common.crdt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CRDT文档实现
 * 
 * 基于RGA（Replicated Growable Array）算法的序列CRDT
 * 
 * 核心思想：
 * 1. 每个字符都有全局唯一的标识符(siteId, clock)
 * 2. 字符按照标识符排序，形成全局一致的顺序
 * 3. 删除操作使用墓碑标记，不真正删除数据
 * 4. 所有操作都是可交换的(commutative)和幂等的(idempotent)
 * 
 * 这保证了无论操作以什么顺序到达，最终状态都是一致的（最终一致性）
 */
@Data
@Slf4j
public class CrdtDocument implements Serializable {
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 站点ID（标识当前客户端）
     */
    private String siteId;
    
    /**
     * 逻辑时钟（Lamport时间戳）
     */
    private AtomicLong clock;
    
    /**
     * 字符存储（使用跳表保证有序和并发安全）
     * Key: 字符ID的字符串表示
     * Value: CRDT字符对象
     */
    private ConcurrentSkipListMap<String, CrdtChar> characters;
    
    /**
     * 字符顺序列表（缓存，用于快速索引访问）
     */
    private volatile List<CrdtChar> orderedChars;
    
    /**
     * 是否需要重建顺序缓存
     */
    private volatile boolean needsRebuild = true;

    /**
     * 虚拟起始字符ID（所有字符都在它之后）
     */
    public static final CharId START_ID = new CharId("__START__", 0);
    
    /**
     * 虚拟结束字符ID（所有字符都在它之前）
     */
    public static final CharId END_ID = new CharId("__END__", Long.MAX_VALUE);

    public CrdtDocument() {
        this.clock = new AtomicLong(0);
        this.characters = new ConcurrentSkipListMap<>();
        this.orderedChars = new ArrayList<>();
    }

    public CrdtDocument(Long documentId, String siteId) {
        this();
        this.documentId = documentId;
        this.siteId = siteId;
    }

    /**
     * 在指定索引位置插入字符
     * 
     * @param index 插入位置（0表示开头）
     * @param value 字符值
     * @return 插入操作
     */
    public CrdtOperation insertAt(int index, String value) {
        long newClock = clock.incrementAndGet();
        CharId newId = new CharId(siteId, newClock);
        
        // 获取前一个字符的ID
        CharId prevId = START_ID;
        List<CrdtChar> visible = getVisibleChars();
        if (index > 0 && index <= visible.size()) {
            prevId = visible.get(index - 1).getId();
        }
        
        CrdtChar newChar = new CrdtChar(newId, value, prevId);
        
        // 应用本地操作
        applyInsert(newChar);
        
        return CrdtOperation.insert(newChar, siteId, newClock, documentId);
    }

    /**
     * 删除指定索引位置的字符
     * 
     * @param index 删除位置
     * @return 删除操作
     */
    public CrdtOperation deleteAt(int index) {
        List<CrdtChar> visible = getVisibleChars();
        if (index < 0 || index >= visible.size()) {
            return null;
        }
        
        CrdtChar targetChar = visible.get(index);
        long newClock = clock.incrementAndGet();
        
        // 应用本地删除
        applyDelete(targetChar.getId());
        
        return CrdtOperation.delete(targetChar.getId(), siteId, newClock, documentId);
    }

    /**
     * 格式化指定范围的字符
     * 
     * @param startIndex 起始索引
     * @param endIndex 结束索引
     * @param attributes 格式属性
     * @return 格式化操作列表
     */
    public List<CrdtOperation> formatRange(int startIndex, int endIndex, Map<String, Object> attributes) {
        List<CrdtOperation> operations = new ArrayList<>();
        List<CrdtChar> visible = getVisibleChars();
        
        for (int i = startIndex; i < endIndex && i < visible.size(); i++) {
            CrdtChar c = visible.get(i);
            long newClock = clock.incrementAndGet();
            
            // 合并属性
            if (c.getAttributes() == null) {
                c.setAttributes(new HashMap<>());
            }
            c.getAttributes().putAll(attributes);
            
            operations.add(CrdtOperation.format(c.getId(), attributes, siteId, newClock, documentId));
        }
        
        return operations;
    }

    /**
     * 应用远程操作
     * 
     * @param operation 远程操作
     */
    public void applyRemoteOperation(CrdtOperation operation) {
        // 更新本地时钟（Lamport时间戳规则）
        clock.updateAndGet(current -> Math.max(current, operation.getClock()) + 1);
        
        switch (operation.getType()) {
            case INSERT:
                applyInsert(operation.getCharacter());
                break;
            case DELETE:
                applyDelete(operation.getTargetId());
                break;
            case FORMAT:
                applyFormat(operation.getTargetId(), operation.getAttributes());
                break;
        }
    }

    /**
     * 应用插入操作
     */
    private void applyInsert(CrdtChar newChar) {
        String key = newChar.getId().toString();
        
        // 幂等性检查：如果已存在则跳过
        if (characters.containsKey(key)) {
            return;
        }
        
        characters.put(key, newChar);
        needsRebuild = true;
        
        log.debug("Applied insert: {} at position after {}", newChar.getValue(), newChar.getPrevId());
    }

    /**
     * 应用删除操作
     */
    private void applyDelete(CharId targetId) {
        String key = targetId.toString();
        CrdtChar target = characters.get(key);
        
        if (target != null && !target.isDeleted()) {
            target.setDeleted(true);
            needsRebuild = true;
            log.debug("Applied delete: {}", targetId);
        }
    }

    /**
     * 应用格式化操作
     */
    private void applyFormat(CharId targetId, Map<String, Object> attributes) {
        String key = targetId.toString();
        CrdtChar target = characters.get(key);
        
        if (target != null) {
            if (target.getAttributes() == null) {
                target.setAttributes(new HashMap<>());
            }
            target.getAttributes().putAll(attributes);
            log.debug("Applied format to {}: {}", targetId, attributes);
        }
    }

    /**
     * 获取可见字符列表（排除已删除的）
     */
    public List<CrdtChar> getVisibleChars() {
        if (needsRebuild) {
            rebuildOrderedChars();
        }
        
        List<CrdtChar> visible = new ArrayList<>();
        for (CrdtChar c : orderedChars) {
            if (!c.isDeleted()) {
                visible.add(c);
            }
        }
        return visible;
    }

    /**
     * 重建有序字符列表
     * 使用拓扑排序，根据prevId关系确定顺序
     */
    private synchronized void rebuildOrderedChars() {
        if (!needsRebuild) {
            return;
        }
        
        // 构建邻接表
        Map<String, List<CrdtChar>> childrenMap = new HashMap<>();
        childrenMap.put(START_ID.toString(), new ArrayList<>());
        
        for (CrdtChar c : characters.values()) {
            String prevKey = c.getPrevId() != null ? c.getPrevId().toString() : START_ID.toString();
            childrenMap.computeIfAbsent(prevKey, k -> new ArrayList<>()).add(c);
        }
        
        // 对每个位置的子节点按ID排序（保证确定性顺序）
        for (List<CrdtChar> children : childrenMap.values()) {
            children.sort((a, b) -> a.getId().compareTo(b.getId()));
        }
        
        // DFS遍历构建顺序
        List<CrdtChar> result = new ArrayList<>();
        buildOrder(START_ID.toString(), childrenMap, result);
        
        this.orderedChars = result;
        this.needsRebuild = false;
    }

    /**
     * DFS构建顺序
     */
    private void buildOrder(String currentKey, Map<String, List<CrdtChar>> childrenMap, List<CrdtChar> result) {
        List<CrdtChar> children = childrenMap.get(currentKey);
        if (children == null) {
            return;
        }
        
        for (CrdtChar child : children) {
            result.add(child);
            buildOrder(child.getId().toString(), childrenMap, result);
        }
    }

    /**
     * 获取文档纯文本内容
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (CrdtChar c : getVisibleChars()) {
            sb.append(c.getValue());
        }
        return sb.toString();
    }

    /**
     * 获取带格式的内容（Quill Delta格式）
     */
    public List<Map<String, Object>> getDelta() {
        List<Map<String, Object>> delta = new ArrayList<>();
        List<CrdtChar> visible = getVisibleChars();
        
        StringBuilder currentText = new StringBuilder();
        Map<String, Object> currentAttrs = null;
        
        for (CrdtChar c : visible) {
            Map<String, Object> attrs = c.getAttributes();
            
            // 如果属性相同，合并文本
            if (Objects.equals(attrs, currentAttrs)) {
                currentText.append(c.getValue());
            } else {
                // 属性不同，保存当前段并开始新段
                if (currentText.length() > 0) {
                    Map<String, Object> op = new HashMap<>();
                    op.put("insert", currentText.toString());
                    if (currentAttrs != null && !currentAttrs.isEmpty()) {
                        op.put("attributes", currentAttrs);
                    }
                    delta.add(op);
                }
                currentText = new StringBuilder(c.getValue());
                currentAttrs = attrs;
            }
        }
        
        // 保存最后一段
        if (currentText.length() > 0) {
            Map<String, Object> op = new HashMap<>();
            op.put("insert", currentText.toString());
            if (currentAttrs != null && !currentAttrs.isEmpty()) {
                op.put("attributes", currentAttrs);
            }
            delta.add(op);
        }
        
        return delta;
    }

    /**
     * 从纯文本初始化文档
     */
    public void initFromText(String text) {
        // 清空现有内容
        characters.clear();
        clock.set(0);
        needsRebuild = true;
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        CharId prevId = START_ID;
        for (char c : text.toCharArray()) {
            long newClock = clock.incrementAndGet();
            CharId newId = new CharId(siteId, newClock);
            CrdtChar newChar = new CrdtChar(newId, String.valueOf(c), prevId);
            characters.put(newId.toString(), newChar);
            prevId = newId;
        }
        needsRebuild = true;
    }

    /**
     * 获取当前时钟值
     */
    public long getCurrentClock() {
        return clock.get();
    }

    /**
     * 合并另一个CRDT文档
     */
    public void merge(CrdtDocument other) {
        for (CrdtChar c : other.characters.values()) {
            String key = c.getId().toString();
            CrdtChar existing = characters.get(key);
            
            if (existing == null) {
                characters.put(key, c);
            } else if (c.isDeleted() && !existing.isDeleted()) {
                existing.setDeleted(true);
            }
            
            // 合并属性
            if (c.getAttributes() != null && existing != null) {
                if (existing.getAttributes() == null) {
                    existing.setAttributes(new HashMap<>());
                }
                existing.getAttributes().putAll(c.getAttributes());
            }
        }
        
        // 更新时钟
        clock.updateAndGet(current -> Math.max(current, other.getCurrentClock()));
        needsRebuild = true;
    }
}
