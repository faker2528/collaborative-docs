package com.collab.user.service;

import com.collab.common.dto.LoginRequest;
import com.collab.common.dto.LoginResponse;
import com.collab.common.dto.RegisterRequest;
import com.collab.common.dto.UserDTO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    UserDTO register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 退出登录
     */
    void logout(Long userId);
    
    /**
     * 获取用户信息
     */
    UserDTO getUserInfo(Long userId);
    
    /**
     * 根据用户名获取用户
     */
    UserDTO getByUsername(String username);
    
    /**
     * 搜索用户（根据用户名模糊搜索）
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID（排除自己）
     * @return 用户列表
     */
    java.util.List<UserDTO> searchUsers(String keyword, Long currentUserId);
    
    /**
     * 更新用户资料
     */
    UserDTO updateProfile(Long userId, com.collab.common.dto.UpdateProfileRequest request);
}
