package com.collab.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
@TableName("t_system_notification")
public class SystemNotification {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 通知类型: 1-平台通知, 2-功能更新, 3-文档操作提醒, 4-权限变更
     */
    private Integer type;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 接收者ID（null表示全体用户）
     */
    private Long receiverId;
    
    /**
     * 关联业务类型: document-文档, user-用户
     */
    private String refType;
    
    /**
     * 关联业务ID
     */
    private Long refId;
    
    /**
     * 跳转链接
     */
    private String link;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
