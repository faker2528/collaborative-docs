package com.collab.file.controller;

import com.collab.common.result.Result;
import com.collab.file.dto.FileRecordDTO;
import com.collab.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    
    private final FileService fileService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileRecordDTO> upload(
            @RequestParam(value = "file") MultipartFile file,
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) Long businessId) {
        FileRecordDTO record = fileService.upload(file, userId, businessType, businessId);
        return Result.success("上传成功", record);
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable(value = "fileId") Long fileId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        FileRecordDTO fileInfo = fileService.getFileInfo(fileId);
        InputStream inputStream = fileService.download(fileId, userId);
        
        String encodedFileName = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(fileInfo.getFileType()))
                .body(new InputStreamResource(inputStream));
    }
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public Result<FileRecordDTO> getFileInfo(@PathVariable(value = "fileId") Long fileId) {
        FileRecordDTO record = fileService.getFileInfo(fileId);
        return Result.success(record);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public Result<Void> delete(
            @PathVariable(value = "fileId") Long fileId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        fileService.delete(fileId, userId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取用户上传的文件列表
     */
    @GetMapping("/list")
    public Result<List<FileRecordDTO>> getUserFiles(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        List<FileRecordDTO> list = fileService.getUserFiles(userId, businessType, page, size);
        return Result.success(list);
    }
}
