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
    
    private String userId;  // 改为 String，防止前端精度丢失
    private String username;
    private String nickname;
    private String avatar;
    private String token;
    private Long expireTime;  // expireTime 可以保持 Long，因为过期时间不会太大
}
