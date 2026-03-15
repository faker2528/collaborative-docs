package com.collab.history.service;

import com.collab.common.entity.DocumentHistory;

import java.util.List;

/**
 * 文档历史服务接口
 */
public interface DocumentHistoryService {
    
    /**
     * 保存文档版本
     */
    DocumentHistory saveVersion(String documentId, String content, Integer operationType,
                                 String operationDesc, String operatorId, String operatorName);
    
    /**
     * 获取文档历史版本列表
     */
    List<DocumentHistory> getVersionList(String documentId);
    
    /**
     * 获取指定版本
     */
    DocumentHistory getVersion(String documentId, Integer version);
    
    /**
     * 回滚到指定版本
     */
    DocumentHistory rollbackToVersion(String documentId, Integer version, String operatorId, String operatorName);
    
    /**
     * 获取最新版本号
     */
    Integer getLatestVersion(String documentId);
}
