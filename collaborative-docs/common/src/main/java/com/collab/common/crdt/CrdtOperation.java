package com.collab.common.crdt;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * CRDT操作定义
 * 
 * 支持三种操作类型：
 * 1. INSERT - 插入字符
 * 2. DELETE - 删除字符（标记墓碑）
 * 3. FORMAT - 格式化操作（修改属性）
 */
@Data
public class CrdtOperation implements Serializable {
    
    /**
     * 操作类型
     */
    private OperationType type;
    
    /**
     * 操作的字符（INSERT时使用）
     */
    private CrdtChar character;
    
    /**
     * 目标字符ID（DELETE/FORMAT时使用）
     */
    private CharId targetId;
    
    /**
     * 格式属性（FORMAT时使用）
     */
    private Map<String, Object> attributes;
    
    /**
     * 操作发起者
     */
    private String siteId;
    
    /**
     * 操作时的逻辑时钟
     */
    private long clock;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 操作时间戳
     */
    private long timestamp;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        INSERT,
        DELETE,
        FORMAT
    }

    /**
     * 创建插入操作
     */
    public static CrdtOperation insert(CrdtChar character, String siteId, long clock, Long documentId) {
        CrdtOperation op = new CrdtOperation();
        op.setType(OperationType.INSERT);
        op.setCharacter(character);
        op.setSiteId(siteId);
        op.setClock(clock);
        op.setDocumentId(documentId);
        op.setTimestamp(System.currentTimeMillis());
        return op;
    }

    /**
     * 创建删除操作
     */
    public static CrdtOperation delete(CharId targetId, String siteId, long clock, Long documentId) {
        CrdtOperation op = new CrdtOperation();
        op.setType(OperationType.DELETE);
        op.setTargetId(targetId);
        op.setSiteId(siteId);
        op.setClock(clock);
        op.setDocumentId(documentId);
        op.setTimestamp(System.currentTimeMillis());
        return op;
    }

    /**
     * 创建格式化操作
     */
    public static CrdtOperation format(CharId targetId, Map<String, Object> attributes, 
                                        String siteId, long clock, Long documentId) {
        CrdtOperation op = new CrdtOperation();
        op.setType(OperationType.FORMAT);
        op.setTargetId(targetId);
        op.setAttributes(attributes);
        op.setSiteId(siteId);
        op.setClock(clock);
        op.setDocumentId(documentId);
        op.setTimestamp(System.currentTimeMillis());
        return op;
    }
}
