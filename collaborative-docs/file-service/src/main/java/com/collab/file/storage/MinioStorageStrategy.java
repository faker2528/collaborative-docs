package com.collab.file.storage;

import com.collab.file.config.FileStorageProperties;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MinIO存储策略实现
 */
@Slf4j
@Component
public class MinioStorageStrategy implements StorageStrategy {
    
    private final FileStorageProperties properties;
    private MinioClient minioClient;
    
    public MinioStorageStrategy(FileStorageProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        FileStorageProperties.MinioConfig minioConfig = properties.getStorage().getMinio();
        if (minioConfig.getEndpoint() != null && !minioConfig.getEndpoint().isEmpty()) {
            try {
                this.minioClient = MinioClient.builder()
                        .endpoint(minioConfig.getEndpoint())
                        .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                        .build();
                
                // 确保bucket存在
                String bucket = minioConfig.getBucket();
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("已创建MinIO存储桶: {}", bucket);
                }
                log.info("MinIO客户端初始化成功");
            } catch (Exception e) {
                log.warn("MinIO客户端初始化失败: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public int getStorageType() {
        return 2;
    }
    
    @Override
    public String upload(InputStream inputStream, String storedName, String contentType) throws Exception {
        if (minioClient == null) {
            throw new IllegalStateException("MinIO客户端未初始化");
        }
        
        String bucket = properties.getStorage().getMinio().getBucket();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = dateDir + "/" + storedName;
        
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, -1, 10485760) // 10MB part size
                .contentType(contentType)
                .build());
        
        log.info("文件已上传到MinIO: {}/{}", bucket, objectName);
        return objectName;
    }
    
    @Override
    public InputStream download(String filePath) throws Exception {
        if (minioClient == null) {
            throw new IllegalStateException("MinIO客户端未初始化");
        }
        
        String bucket = properties.getStorage().getMinio().getBucket();
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(filePath)
                .build());
    }
    
    @Override
    public void delete(String filePath) throws Exception {
        if (minioClient == null) {
            throw new IllegalStateException("MinIO客户端未初始化");
        }
        
        String bucket = properties.getStorage().getMinio().getBucket();
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(filePath)
                .build());
        
        log.info("文件已从MinIO删除: {}/{}", bucket, filePath);
    }
    
    @Override
    public String getUrl(String filePath) {
        FileStorageProperties.MinioConfig minioConfig = properties.getStorage().getMinio();
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + filePath;
    }
}

