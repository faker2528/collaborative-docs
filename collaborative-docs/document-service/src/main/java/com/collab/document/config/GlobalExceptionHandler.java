package com.collab.document.config;

import cn.hutool.core.util.StrUtil;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.Result;
import com.collab.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器（增强版）
 * 
 * 特性：
 * 1. 精准定位业务代码位置（过滤 Spring 框架堆栈）
 * 2. 记录请求上下文（URL、参数、方法）
 * 3. 结构化日志输出
 * 4. 异常信息摘要提取
 */
@Slf4j
@RestControllerAdvice
@Component
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(HttpServletRequest request, BusinessException e) {
        // 获取业务代码位置
        String location = findFirstUserStackTrace(e.getStackTrace());
        
        log.warn("====== 业务异常 ======");
        log.warn("接口：{} {}", request.getMethod(), request.getRequestURI());
        log.warn("位置：{}", location);
        log.warn("错误码：{}", e.getCode());
        log.warn("消息：{}", e.getMessage());
        log.warn("====================");
        
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(HttpServletRequest request, MethodArgumentNotValidException e) {
        String location = findFirstUserStackTrace(e.getStackTrace());
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> StrUtil.format("{}: {}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        
        log.warn("====== 参数校验失败 ======");
        log.warn("接口：{} {}", request.getMethod(), request.getRequestURI());
        log.warn("位置：{}", location);
        log.warn("失败字段：{}", fieldErrors.stream().map(FieldError::getField).collect(Collectors.joining(", ")));
        log.warn("错误详情：{}", errorMessage);
        log.warn("========================");
        
        return Result.error(ResultCode.PARAM_NOT_VALID.getCode(), errorMessage);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(HttpServletRequest request, BindException e) {
        String location = findFirstUserStackTrace(e.getStackTrace());
        
        List<FieldError> fieldErrors = e.getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> StrUtil.format("{}: {}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        
        log.warn("====== 参数绑定失败 ======");
        log.warn("接口：{} {}", request.getMethod(), request.getRequestURI());
        log.warn("位置：{}", location);
        log.warn("失败字段：{}", fieldErrors.stream().map(FieldError::getField).collect(Collectors.joining(", ")));
        log.warn("错误详情：{}", errorMessage);
        log.warn("========================");
        
        return Result.error(ResultCode.PARAM_NOT_VALID.getCode(), errorMessage);
    }

    /**
     * 处理所有未捕获的异常（增强版）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(HttpServletRequest request, Exception e) {
        // 获取业务代码位置
        String location = findFirstUserStackTrace(e.getStackTrace());
        
        // 提取关键堆栈信息（前 5 行）
        String shortStackTrace = extractShortStackTrace(e.getStackTrace(), 5);
        
        // 完整堆栈（用于问题排查）
        String fullStackTrace = getFullStackTrace(e);
        
        // 异常原因链
        String causeChain = getCauseChain(e);
        
        log.error("====== 系统异常 ======", e);
        log.error("时间：{}", java.time.LocalDateTime.now());
        log.error("接口：{} {}", request.getMethod(), request.getRequestURI());
        log.error("位置：{}", location);
        log.error("异常类型：{}", e.getClass().getName());
        log.error("异常消息：{}", e.getMessage());
        log.error("异常原因链：\n{}", causeChain);
        log.error("关键堆栈（前 5 行）：\n{}", shortStackTrace);
        log.error("====================");
        
        // 开发环境返回详细信息
        if (isDevEnvironment()) {
            return Result.error("系统异常：" + e.getMessage() + "\n位置：" + location);
        }
        
        // 生产环境只返回通用错误
        return Result.error("系统内部错误，请稍后重试");
    }

    /**
     * 找到第一个用户代码的堆栈位置（过滤 Spring、Tomcat 等框架）
     */
    private String findFirstUserStackTrace(StackTraceElement[] stackTrace) {
        // 需要过滤的包名前缀
        List<String> frameworkPrefixes = Arrays.asList(
            "org.springframework.",
            "org.apache.catalina.",
            "org.apache.tomcat.",
            "jakarta.servlet.",
            "sun.reflect.",
            "java.lang.reflect.",
            "jdk.internal.reflect.",
            "org.apache.coyote."
        );
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // 跳过框架代码
            boolean isFramework = frameworkPrefixes.stream()
                    .anyMatch(prefix -> className.startsWith(prefix));
            
            // 跳过包含 Filter、Servlet、Proxy 的类
            if (className.contains("Filter") || 
                className.contains("Servlet") || 
                className.contains("$Proxy") ||
                className.contains("Interceptor")) {
                continue;
            }
            
            if (!isFramework) {
                // 返回第一个非框架代码位置
                return StrUtil.format("{}.{}({}:{})", 
                        element.getClassName(),
                        element.getMethodName(),
                        element.getFileName() != null ? element.getFileName() : "Unknown",
                        element.getLineNumber());
            }
        }
        
        // 如果找不到，返回堆栈第一个元素
        if (stackTrace.length > 0) {
            StackTraceElement first = stackTrace[0];
            return StrUtil.format("{}.{}({}:{})", 
                    first.getClassName(),
                    first.getMethodName(),
                    first.getFileName() != null ? first.getFileName() : "Unknown",
                    first.getLineNumber());
        }
        
        return "未知位置";
    }

    /**
     * 提取简短的堆栈信息（指定行数）
     */
    private String extractShortStackTrace(StackTraceElement[] stackTrace, int maxLines) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        for (StackTraceElement element : stackTrace) {
            if (count >= maxLines) {
                break;
            }
            
            // 只显示用户代码
            String className = element.getClassName();
            boolean isFramework = className.startsWith("org.springframework.") || 
                                 className.startsWith("org.apache.catalina.") ||
                                 className.startsWith("org.apache.tomcat.") ||
                                 className.startsWith("jakarta.servlet.") ||
                                 className.startsWith("org.apache.coyote.");
            
            // 跳过 Filter、Servlet 等
            if (className.contains("Filter") || 
                className.contains("Servlet") ||
                className.contains("Interceptor")) {
                continue;
            }
            
            if (!isFramework) {
                sb.append(StrUtil.format("\tat {}.{}({}:{})\n",
                        element.getClassName(),
                        element.getMethodName(),
                        element.getFileName() != null ? element.getFileName() : "Unknown",
                        element.getLineNumber()));
                count++;
            }
        }
        
        if (stackTrace.length > maxLines) {
            sb.append("\t... (还有 ").append(stackTrace.length - maxLines).append(" 行)\n");
        }
        
        return sb.toString();
    }

    /**
     * 获取完整的堆栈跟踪字符串
     */
    private String getFullStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取异常原因链
     */
    private String getCauseChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable cause = e.getCause();
        
        int depth = 0;
        while (cause != null && depth < 5) {
            sb.append(StrUtil.format("  Caused by {}: {}\n", 
                    cause.getClass().getSimpleName(), 
                    cause.getMessage()));
            cause = cause.getCause();
            depth++;
        }
        
        return sb.toString();
    }

    /**
     * 判断是否是开发环境
     */
    private boolean isDevEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active", 
                                   System.getenv("SPRING_PROFILES_ACTIVE"));
        return "dev".equals(activeProfile) || "local".equals(activeProfile);
    }
}
