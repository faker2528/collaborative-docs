package com.collab.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.collab.common.constant.RedisConstant;
import com.collab.common.result.Result;
import com.collab.common.result.ResultCode;
import com.collab.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证全局过滤器
 * 
 * 功能：
 * 1. 拦截所有请求，检查JWT Token
 * 2. 白名单路径直接放行
 * 3. 验证Token有效性
 * 4. 将用户信息传递给下游服务
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 白名单路径（无需认证）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/api/share/info/**",
            "/ws/**",
            "/actuator/**"
    );

    public JwtAuthFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        log.debug("Gateway filter - Path: {}, Method: {}", path, request.getMethod());

        // 白名单放行
        if (isWhiteListed(path)) {
            log.debug("Path {} is in white list, pass through", path);
            return chain.filter(exchange);
        }

        // 获取Token
        String token = getToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("No token found for path: {}", path);
            return unauthorized(exchange, "未提供认证Token");
        }

        // 验证Token
        try {
            if (!JwtUtil.validateToken(token)) {
                log.warn("Invalid token for path: {}", path);
                return unauthorized(exchange, "Token无效或已过期");
            }

            // 从Token中获取用户信息
            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);

            // 验证Token是否在Redis中（防止Token被注销后继续使用）
            String redisKey = RedisConstant.TOKEN_PREFIX + userId;
            return redisTemplate.hasKey(redisKey)
                    .flatMap(exists -> {
                        if (Boolean.FALSE.equals(exists)) {
                            log.warn("Token not found in Redis for user: {}", userId);
                            return unauthorized(exchange, "Token已失效，请重新登录");
                        }

                        // 将用户信息添加到请求头，传递给下游服务
                        ServerHttpRequest newRequest = request.mutate()
                                .header("X-User-Id", String.valueOf(userId))
                                .header("X-Username", username)
                                .build();

                        log.debug("Token validated for user: {}, passing to downstream", username);
                        return chain.filter(exchange.mutate().request(newRequest).build());
                    });

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return unauthorized(exchange, "Token验证失败");
        }
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求中获取Token
     * 支持两种方式：
     * 1. Authorization Header: Bearer {token}
     * 2. Query Parameter: token={token}
     */
    private String getToken(ServerHttpRequest request) {
        // 先从Header获取
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 再从Query参数获取（用于WebSocket连接）
        String tokenParam = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<?> result = Result.error(ResultCode.UNAUTHORIZED.getCode(), message);
        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 最高优先级
        return -100;
    }
}
