package com.collab.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.constant.RedisConstant;
import com.collab.common.dto.LoginRequest;
import com.collab.common.dto.LoginResponse;
import com.collab.common.dto.RegisterRequest;
import com.collab.common.dto.UpdateProfileRequest;
import com.collab.common.dto.UserDTO;
import com.collab.common.entity.User;
import com.collab.common.exception.BusinessException;
import com.collab.common.result.ResultCode;
import com.collab.common.util.JwtUtil;
import com.collab.user.mapper.UserMapper;
import com.collab.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        // 使用BCrypt加密密码
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        user.setDeleted(0);

        userMapper.insert(user);
        log.info("User registered successfully: {}", user.getUsername());

        return convertToDTO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException("【用户已被禁用】");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        long expireTime = System.currentTimeMillis() + RedisConstant.TOKEN_EXPIRE_TIME * 1000;

        // 将Token存储到Redis
        String redisKey = RedisConstant.TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, RedisConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);

        log.info("【User logged in successfully】: {}", user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .expireTime(expireTime)
                .build();
    }

    @Override
    public void logout(Long userId) {
        // 从Redis中删除Token
        String redisKey = RedisConstant.TOKEN_PREFIX + userId;
        redisTemplate.delete(redisKey);
        log.info("User logged out: {}", userId);
    }

    @Override
    public UserDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToDTO(user);
    }

    @Override
    public UserDTO getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }
        return convertToDTO(user);
    }
    
    @Override
    public List<UserDTO> searchUsers(String keyword, Long currentUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 模糊搜索用户名或昵称
        wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
        );
        // 排除当前用户
        wrapper.ne(User::getId, currentUserId);
        // 只查询启用状态的用户
        wrapper.eq(User::getStatus, 1);
        // 限制返回数量
        wrapper.last("LIMIT 10");
        
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::convertToDTO).toList();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 更新资料
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        userMapper.updateById(user);
        log.info("User {} updated profile", userId);
        
        return convertToDTO(user);
    }

    /**
     * 将User实体转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(String.valueOf(user.getId()));
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
