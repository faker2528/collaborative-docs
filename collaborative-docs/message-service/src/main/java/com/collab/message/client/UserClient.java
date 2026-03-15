package com.collab.message.client;

import com.collab.common.dto.UserDTO;
import com.collab.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserClient {
    
    @GetMapping("/{userId}")
    Result<UserDTO> getUserById(@PathVariable(value = "userId") String userId);
}
