package com.collab.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友请求实体
 */
@Data
@TableName("t_friend_request")
public class FriendRequest implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 发送者ID
     */
    private Long fromUserId;
    
    /**
     * 接收者ID
     */
    private Long toUserId;
    
    /**
     * 附加消息
     */
    private String message;
    
    /**
     * 状态: 0-待处理, 1-已同意, 2-已拒绝
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
