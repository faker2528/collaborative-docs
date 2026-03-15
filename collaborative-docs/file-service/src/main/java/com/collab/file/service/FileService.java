package com.collab.file.service;

import com.collab.file.dto.FileRecordDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传文件
     * @param file 文件
     * @param uploaderId 上传者ID
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 文件记录
     */
    FileRecordDTO upload(MultipartFile file, String uploaderId, String businessType, String businessId);
    
    /**
     * 下载文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件输入流
     */
    InputStream download(String fileId, String userId);
    
    /**
     * 获取文件信息
     * @param fileId 文件ID
     * @return 文件记录
     */
    FileRecordDTO getFileInfo(String fileId);
    
    /**
     * 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    void delete(String fileId, String userId);
    
    /**
     * 获取用户上传的文件列表
     * @param userId 用户ID
     * @param businessType 业务类型（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileRecordDTO> getUserFiles(String userId, String businessType, Integer page, Integer size);
}
