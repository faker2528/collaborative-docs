package com.collab.file.storage;

import java.io.InputStream;

/**
 * 存储策略接口
 */
public interface StorageStrategy {
    
    /**
     * 获取存储类型: 1-本地, 2-MinIO, 3-OSS
     */
    int getStorageType();
    
    /**
     * 上传文件
     * @param inputStream 文件输入流
     * @param storedName 存储文件名
     * @param contentType 文件类型
     * @return 文件存储路径
     */
    String upload(InputStream inputStream, String storedName, String contentType) throws Exception;
    
    /**
     * 下载文件
     * @param filePath 文件路径
     * @return 文件输入流
     */
    InputStream download(String filePath) throws Exception;
    
    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void delete(String filePath) throws Exception;
    
    /**
     * 获取文件访问URL
     * @param filePath 文件路径
     * @return 访问URL
     */
    String getUrl(String filePath);
}
