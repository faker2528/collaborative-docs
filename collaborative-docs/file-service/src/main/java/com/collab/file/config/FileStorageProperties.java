package com.collab.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    
    private StorageConfig storage = new StorageConfig();
    private List<String> allowedTypes;
    private Long maxSize;
    
    @Data
    public static class StorageConfig {
        private String type = "local";
        private LocalConfig local = new LocalConfig();
        private MinioConfig minio = new MinioConfig();
    }
    
    @Data
    public static class LocalConfig {
        private String path = "./uploads";
        private String urlPrefix = "/api/file/download/";
    }
    
    @Data
    public static class MinioConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
