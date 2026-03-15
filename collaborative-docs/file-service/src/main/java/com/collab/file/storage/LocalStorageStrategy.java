package com.collab.file.storage;

import com.collab.file.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地存储策略实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageStrategy implements StorageStrategy {
    
    private final FileStorageProperties properties;
    
    @Override
    public int getStorageType() {
        return 1;
    }
    
    @Override
    public String upload(InputStream inputStream, String storedName, String contentType) throws Exception {
        // 按日期分目录存储
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = properties.getStorage().getLocal().getPath();
        Path dirPath = Paths.get(basePath, dateDir);
        
        // 创建目录
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // 存储文件
        Path filePath = dirPath.resolve(storedName);
        try (OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        log.info("【文件上传】文件已保存到本地: {}", filePath);
        return dateDir + "/" + storedName;
    }
    
    @Override
    public InputStream download(String filePath) throws Exception {
        String basePath = properties.getStorage().getLocal().getPath();
        Path path = Paths.get(basePath, filePath);
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("【文件下载】文件不存在: " + filePath);
        }
        
        return new FileInputStream(path.toFile());
    }
    
    @Override
    public void delete(String filePath) throws Exception {
        String basePath = properties.getStorage().getLocal().getPath();
        Path path = Paths.get(basePath, filePath);
        
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("【文件删除】文件已删除: {}", path);
        }
    }
    
    @Override
    public String getUrl(String filePath) {
        String urlPrefix = properties.getStorage().getLocal().getUrlPrefix();
        return urlPrefix + filePath;
    }
}

