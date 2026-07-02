package com.smartfarm.framework.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置
 *
 * <pre>
 * 端点: /ws (SockJS 兼容)
 * 应用前缀: /app (客户端发送消息到此)
 * Broker前缀: /topic (服务端推送消息到此)
 *
 * Topic规划:
 *   /topic/plot/{plotId}/sensors  — 传感器实时数据
 *   /topic/plot/{plotId}/alerts   — 告警通知
 *   /topic/device/{id}/status     — 设备状态 + 指令结果
 * </pre>
 *
 * @author SmartFarm Team
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅的前缀（服务端 → 客户端）
        registry.enableSimpleBroker("/topic");
        // 客户端发送消息的前缀（客户端 → 服务端）
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点，允许跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
