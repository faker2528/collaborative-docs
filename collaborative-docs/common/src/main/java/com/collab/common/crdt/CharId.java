package com.collab.common.crdt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * CRDT字符标识符
 * 
 * 为什么选择CRDT而不是OT（Operational Transformation）?
 * 
 * 1. 去中心化：CRDT不需要中央服务器来协调操作顺序，每个节点可以独立处理操作
 * 2. 最终一致性保证：CRDT通过数学性质保证所有节点最终达到相同状态，无需复杂的转换函数
 * 3. 实现简单：OT需要为每对操作类型实现转换函数，复杂度为O(n²)，CRDT只需简单的合并规则
 * 4. 无需操作历史：OT需要维护操作历史来进行转换，CRDT可以直接合并任意状态
 * 5. 离线支持：CRDT天然支持离线编辑后同步，OT处理离线冲突更复杂
 * 
 * 本实现使用基于唯一标识符的序列CRDT（类似RGA算法）
 * 每个字符都有一个全局唯一的标识符(siteId, clock)
 * 通过比较标识符来确定字符的全局顺序
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharId implements Comparable<CharId>, Serializable {
    
    /**
     * 站点ID（用户/客户端唯一标识）
     */
    private String siteId;
    
    /**
     * 逻辑时钟（Lamport时间戳）
     */
    private long clock;

    /**
     * 比较两个字符ID的顺序
     * 先比较clock，clock相同则比较siteId（字典序）
     * 这保证了全局唯一的全序关系
     */
    @Override
    public int compareTo(CharId other) {
        if (this.clock != other.clock) {
            return Long.compare(this.clock, other.clock);
        }
        return this.siteId.compareTo(other.siteId);
    }

    @Override
    public String toString() {
        return siteId + ":" + clock;
    }
    
    /**
     * 从字符串解析CharId
     */
    public static CharId fromString(String str) {
        String[] parts = str.split(":");
        return new CharId(parts[0], Long.parseLong(parts[1]));
    }
}
