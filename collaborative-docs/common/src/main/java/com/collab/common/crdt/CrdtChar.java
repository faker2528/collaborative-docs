package com.collab.common.crdt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * CRDT字符节点
 * 包含字符内容、唯一标识符和格式信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrdtChar implements Serializable {
    
    /**
     * 字符的唯一标识符
     */
    private CharId id;
    
    /**
     * 字符内容（单个字符或特殊标记）
     */
    private String value;
    
    /**
     * 是否已删除（墓碑标记）
     * CRDT中删除不是真的删除，而是标记为已删除
     * 这保证了操作的可交换性
     */
    private boolean deleted;
    
    /**
     * 前一个字符的ID（用于确定插入位置）
     */
    private CharId prevId;
    
    /**
     * 格式属性（如bold、italic等）
     */
    private Map<String, Object> attributes;

    public CrdtChar(CharId id, String value, CharId prevId) {
        this.id = id;
        this.value = value;
        this.prevId = prevId;
        this.deleted = false;
    }

    /**
     * 创建一个已删除的副本
     */
    public CrdtChar markDeleted() {
        CrdtChar copy = new CrdtChar();
        copy.setId(this.id);
        copy.setValue(this.value);
        copy.setDeleted(true);
        copy.setPrevId(this.prevId);
        copy.setAttributes(this.attributes);
        return copy;
    }
}
