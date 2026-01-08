package com.collab.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String token;
    private Long expireTime;
}
