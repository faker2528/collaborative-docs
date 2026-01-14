package com.collab.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录实体
 */
@Data
@TableName("t_file_record")
public class FileRecord {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 存储文件名
     */
    private String storedName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 文件类型(MIME)
     */
    private String fileType;
    
    /**
     * 文件扩展名
     */
    private String fileExt;
    
    /**
     * 存储类型: 1-本地, 2-MinIO, 3-OSS
     */
    private Integer storageType;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 上传者ID
     */
    private Long uploaderId;
    
    /**
     * 业务类型: chat-聊天, document-文档, avatar-头像
     */
    private String businessType;
    
    /**
     * 关联业务ID
     */
    private Long businessId;
    
    /**
     * 下载次数
     */
    private Integer downloadCount;
    
    /**
     * 状态: 0-已删除, 1-正常
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
