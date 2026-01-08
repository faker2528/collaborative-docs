package com.collab.collaboration.interceptor;

import com.collab.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket认证拦截器
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.debug("WebSocket handshake started");
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从URL参数获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket handshake failed: no token provided");
                return false;
            }

            try {
                // 验证token
                if (!JwtUtil.validateToken(token)) {
                    log.warn("WebSocket handshake failed: invalid token");
                    return false;
                }

                // 获取用户信息
                Long userId = JwtUtil.getUserId(token);
                String username = JwtUtil.getUsername(token);

                // 存储到attributes中，供后续使用
                attributes.put("userId", userId);
                attributes.put("username", username);
                
                // 从URL路径中提取documentId
                String path = request.getURI().getPath();
                String[] pathParts = path.split("/");
                if (pathParts.length > 0) {
                    String documentIdStr = pathParts[pathParts.length - 1];
                    attributes.put("documentId", Long.parseLong(documentIdStr));
                }

                log.debug("WebSocket handshake success for user: {}", username);
                return true;

            } catch (Exception e) {
                log.error("WebSocket handshake error: {}", e.getMessage());
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake exception", exception);
        }
    }
}
