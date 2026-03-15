package com.collab.file.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.common.exception.BusinessException;
import com.collab.file.config.FileStorageProperties;
import com.collab.file.dto.FileRecordDTO;
import com.collab.file.entity.FileRecord;
import com.collab.file.mapper.FileRecordMapper;
import com.collab.file.service.FileService;
import com.collab.file.storage.LocalStorageStrategy;
import com.collab.file.storage.MinioStorageStrategy;
import com.collab.file.storage.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    
    private final FileRecordMapper fileRecordMapper;
    private final FileStorageProperties properties;
    private final LocalStorageStrategy localStorageStrategy;
    private final MinioStorageStrategy minioStorageStrategy;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordDTO upload(MultipartFile file, String uploaderId, String businessType, String businessId) {
        // 校验文件
        validateFile(file);
        
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        String fileExt = getFileExtension(originalName);
        
        // 生成存储文件名
        String storedName = IdUtil.fastSimpleUUID() + "." + fileExt;
        
        // 获取存储策略
        StorageStrategy storageStrategy = getStorageStrategy();
        
        try {
            // 上传文件
            String filePath = storageStrategy.upload(file.getInputStream(), storedName, contentType);
            
            // 保存文件记录
            FileRecord record = new FileRecord();
            record.setOriginalName(originalName);
            record.setStoredName(storedName);
            record.setFilePath(filePath);
            record.setFileSize(fileSize);
            record.setFileType(contentType);
            record.setFileExt(fileExt);
            record.setStorageType(storageStrategy.getStorageType());
            record.setUploaderId(uploaderId);
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setDownloadCount(0);
            record.setStatus(1);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            record.setDeleted(0);
            
            fileRecordMapper.insert(record);
            
            log.info("【文件上传】文件上传成功: {} -> {}", originalName, filePath);
            return convertToDTO(record, storageStrategy);
            
        } catch (Exception e) {
            log.error("【文件上传】文件上传失败: {}", originalName, e);
            throw new BusinessException("【文件上传】文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream download(String fileId, String userId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null || record.getStatus() != 1) {
            throw new BusinessException("【文件下载】文件不存在");
        }
        
        try {
            StorageStrategy strategy = getStorageStrategy(record.getStorageType());
            InputStream inputStream = strategy.download(record.getFilePath());
            
            // 增加下载次数
            record.setDownloadCount(record.getDownloadCount() + 1);
            fileRecordMapper.updateById(record);
            
            return inputStream;
        } catch (Exception e) {
            log.error("【文件下载】文件下载失败: {}", fileId, e);
            throw new BusinessException("【文件下载】文件下载失败: " + e.getMessage());
        }
    }
    
    @Override
    public FileRecordDTO getFileInfo(String fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null || record.getStatus() != 1) {
            throw new BusinessException("【文件信息】文件不存在");
        }
        
        StorageStrategy strategy = getStorageStrategy(record.getStorageType());
        return convertToDTO(record, strategy);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fileId, String userId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw new BusinessException("【文件删除】文件不存在");
        }
        
        // 验证权限
        if (!record.getUploaderId().equals(userId)) {
            throw new BusinessException("【文件删除】无权删除此文件");
        }
        
        try {
            // 删除存储的文件
            StorageStrategy strategy = getStorageStrategy(record.getStorageType());
            strategy.delete(record.getFilePath());
            
            // 更新状态
            record.setStatus(0);
            record.setUpdateTime(LocalDateTime.now());
            fileRecordMapper.updateById(record);
            
            log.info("【文件删除】文件已删除: {}", fileId);
        } catch (Exception e) {
            log.error("【文件删除】文件删除失败: {}", fileId, e);
            throw new BusinessException("【文件删除】文件删除失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<FileRecordDTO> getUserFiles(String userId, String businessType, Integer page, Integer size) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getUploaderId, userId)
               .eq(FileRecord::getStatus, 1);
        
        if (businessType != null && !businessType.isEmpty()) {
            wrapper.eq(FileRecord::getBusinessType, businessType);
        }
        
        wrapper.orderByDesc(FileRecord::getCreateTime);
        
        Page<FileRecord> pageResult = fileRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<FileRecordDTO> result = new ArrayList<>();
        
        for (FileRecord record : pageResult.getRecords()) {
            StorageStrategy strategy = getStorageStrategy(record.getStorageType());
            result.add(convertToDTO(record, strategy));
        }
        
        return result;
    }
    
    // ========== 私有方法 ==========
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 校验文件大小
        if (file.getSize() > properties.getMaxSize()) {
            throw new BusinessException("文件大小超出限制");
        }
        
        // 校验文件类型
        String contentType = file.getContentType();
        List<String> allowedTypes = properties.getAllowedTypes();
        if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(contentType)) {
            throw new BusinessException("不支持的文件类型: " + contentType);
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private StorageStrategy getStorageStrategy() {
        String type = properties.getStorage().getType();
        return "minio".equalsIgnoreCase(type) ? minioStorageStrategy : localStorageStrategy;
    }
    
    private StorageStrategy getStorageStrategy(int storageType) {
        return storageType == 2 ? minioStorageStrategy : localStorageStrategy;
    }
    
    private FileRecordDTO convertToDTO(FileRecord record, StorageStrategy strategy) {
        FileRecordDTO dto = new FileRecordDTO();
        dto.setId(record.getId());
        dto.setOriginalName(record.getOriginalName());
        dto.setFileSize(record.getFileSize());
        dto.setFileType(record.getFileType());
        dto.setFileExt(record.getFileExt());
        dto.setStorageType(record.getStorageType());
        dto.setUrl(strategy.getUrl(record.getFilePath()));
        dto.setBusinessType(record.getBusinessType());
        dto.setDownloadCount(record.getDownloadCount());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }
}

