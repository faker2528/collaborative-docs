package com.collab.common.dto;

import lombok.Data;

/**
 * 用户信息DTO
 */
@Data
public class UserDTO {
    
    private String id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private Integer status;
}
