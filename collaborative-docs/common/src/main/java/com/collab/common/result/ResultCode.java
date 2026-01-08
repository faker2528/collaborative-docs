package com.collab.common.result;

import lombok.Getter;

/**
 * 返回状态码
 */
@Getter
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    
    // 认证相关 401xx
    UNAUTHORIZED(40100, "未登录或Token已过期"),
    TOKEN_INVALID(40101, "Token无效"),
    TOKEN_EXPIRED(40102, "Token已过期"),
    
    // 权限相关 403xx
    FORBIDDEN(40300, "没有操作权限"),
    
    // 参数相关 400xx
    PARAM_ERROR(40000, "参数错误"),
    PARAM_NOT_VALID(40001, "参数校验失败"),
    
    // 用户相关 410xx
    USER_NOT_FOUND(41000, "用户不存在"),
    USER_ALREADY_EXISTS(41001, "用户已存在"),
    PASSWORD_ERROR(41002, "密码错误"),
    
    // 文档相关 420xx
    DOCUMENT_NOT_FOUND(42000, "文档不存在"),
    DOCUMENT_NO_PERMISSION(42001, "没有文档操作权限"),
    
    // 协作相关 430xx
    COLLABORATION_ROOM_NOT_FOUND(43000, "协作房间不存在"),
    COLLABORATION_JOIN_FAILED(43001, "加入协作失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
