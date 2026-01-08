package com.collab.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建文档请求
 */
@Data
public class CreateDocumentRequest {
    
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    private String content;
}
