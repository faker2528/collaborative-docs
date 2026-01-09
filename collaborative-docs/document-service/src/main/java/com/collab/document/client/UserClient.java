package com.collab.document.client;

import com.collab.common.dto.UserDTO;
import com.collab.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserClient {
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);
}
