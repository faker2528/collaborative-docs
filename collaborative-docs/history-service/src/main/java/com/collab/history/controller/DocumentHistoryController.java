package com.collab.history.controller;

import com.collab.common.entity.DocumentHistory;
import com.collab.common.result.Result;
import com.collab.history.service.DocumentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档历史控制器
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class DocumentHistoryController {

    private final DocumentHistoryService historyService;

    /**
     * 保存文档版本
     */
    @PostMapping("/save")
    public Result<DocumentHistory> saveVersion(@RequestParam Long documentId,
                                                @RequestBody String content,
                                                @RequestParam(defaultValue = "2") Integer operationType,
                                                @RequestParam(required = false) String operationDesc,
                                                @RequestHeader("X-User-Id") Long userId,
                                                @RequestHeader("X-Username") String username) {
        DocumentHistory history = historyService.saveVersion(documentId, content, operationType, 
                operationDesc, userId, username);
        return Result.success("版本保存成功", history);
    }

    /**
     * 获取文档历史版本列表
     */
    @GetMapping("/{documentId}/list")
    public Result<List<DocumentHistory>> getVersionList(@PathVariable Long documentId) {
        List<DocumentHistory> versions = historyService.getVersionList(documentId);
        return Result.success(versions);
    }

    /**
     * 获取指定版本
     */
    @GetMapping("/{documentId}/version/{version}")
    public Result<DocumentHistory> getVersion(@PathVariable Long documentId,
                                               @PathVariable Integer version) {
        DocumentHistory history = historyService.getVersion(documentId, version);
        return Result.success(history);
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/{documentId}/rollback/{version}")
    public Result<DocumentHistory> rollbackToVersion(@PathVariable Long documentId,
                                                      @PathVariable Integer version,
                                                      @RequestHeader("X-User-Id") Long userId,
                                                      @RequestHeader("X-Username") String username) {
        DocumentHistory history = historyService.rollbackToVersion(documentId, version, userId, username);
        return Result.success("版本回滚成功", history);
    }

    /**
     * 获取最新版本号
     */
    @GetMapping("/{documentId}/latest-version")
    public Result<Integer> getLatestVersion(@PathVariable Long documentId) {
        Integer version = historyService.getLatestVersion(documentId);
        return Result.success(version);
    }
}
