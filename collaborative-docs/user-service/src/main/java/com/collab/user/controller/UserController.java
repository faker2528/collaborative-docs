package com.collab.user.controller;

import com.collab.common.dto.LoginRequest;
import com.collab.common.dto.LoginResponse;
import com.collab.common.dto.RegisterRequest;
import com.collab.common.dto.UpdateProfileRequest;
import com.collab.common.dto.UserDTO;
import com.collab.common.result.Result;
import com.collab.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        return Result.success("注册成功", user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        userService.logout(userId);
        return Result.success("退出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserDTO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        UserDTO user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO user = userService.getUserInfo(userId);
        return Result.success(user);
    }
    
    /**
     * 搜索用户（根据用户名模糊搜索）
     */
    @GetMapping("/search")
    public Result<List<UserDTO>> searchUsers(@RequestParam("keyword") String keyword,
                                              @RequestHeader("X-User-Id") Long userId) {
        List<UserDTO> users = userService.searchUsers(keyword, userId);
        return Result.success(users);
    }
    
    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<UserDTO> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody UpdateProfileRequest request) {
        UserDTO user = userService.updateProfile(userId, request);
        return Result.success("资料更新成功", user);
    }
}
