package com.collab.history.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.entity.DocumentHistory;
import com.collab.common.exception.BusinessException;
import com.collab.history.mapper.DocumentHistoryMapper;
import com.collab.history.service.DocumentHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentHistoryServiceImpl implements DocumentHistoryService {

    private final DocumentHistoryMapper historyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentHistory saveVersion(Long documentId, String content, Integer operationType,
                                        String operationDesc, Long operatorId, String operatorName) {
        // 获取当前最大版本号
        Integer latestVersion = getLatestVersion(documentId);
        int newVersion = latestVersion != null ? latestVersion + 1 : 1;

        DocumentHistory history = new DocumentHistory();
        history.setDocumentId(documentId);
        history.setVersion(newVersion);
        history.setContent(content);
        history.setOperationType(operationType);
        history.setOperationDesc(operationDesc);
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);

        historyMapper.insert(history);
        log.info("Saved document version: documentId={}, version={}", documentId, newVersion);

        return history;
    }

    @Override
    public List<DocumentHistory> getVersionList(Long documentId) {
        LambdaQueryWrapper<DocumentHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentHistory::getDocumentId, documentId)
                .orderByDesc(DocumentHistory::getVersion);
        return historyMapper.selectList(wrapper);
    }

    @Override
    public DocumentHistory getVersion(Long documentId, Integer version) {
        LambdaQueryWrapper<DocumentHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentHistory::getDocumentId, documentId)
                .eq(DocumentHistory::getVersion, version);
        return historyMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentHistory rollbackToVersion(Long documentId, Integer version, Long operatorId, String operatorName) {
        // 获取目标版本
        DocumentHistory targetVersion = getVersion(documentId, version);
        if (targetVersion == null) {
            throw new BusinessException("指定版本不存在");
        }

        // 创建新版本（回滚操作）
        String operationDesc = "回滚到版本 " + version;
        return saveVersion(documentId, targetVersion.getContent(), 3, operationDesc, operatorId, operatorName);
    }

    @Override
    public Integer getLatestVersion(Long documentId) {
        LambdaQueryWrapper<DocumentHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentHistory::getDocumentId, documentId)
                .orderByDesc(DocumentHistory::getVersion)
                .last("LIMIT 1");
        DocumentHistory latest = historyMapper.selectOne(wrapper);
        return latest != null ? latest.getVersion() : null;
    }
}
