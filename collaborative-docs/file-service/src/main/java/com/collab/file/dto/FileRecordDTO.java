package com.collab.file.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件记录DTO
 */
@Data
public class FileRecordDTO {
    
    private String id;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件扩展名
     */
    private String fileExt;
    
    /**
     * 存储类型
     */
    private Integer storageType;
    
    /**
     * 文件访问URL
     */
    private String url;
    
    /**
     * 业务类型
     */
    private String businessType;
    
    /**
     * 下载次数
     */
    private Integer downloadCount;
    
    /**
     * 上传时间
     */
    private LocalDateTime createTime;
}
