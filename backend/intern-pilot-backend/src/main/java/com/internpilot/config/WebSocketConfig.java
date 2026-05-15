package com.internpilot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.swagger.v3.oas.annotations.media.Schema;

@Configuration
@EnableWebSocketMessageBroker//启用WebSocket消息代理功能
@Schema(description = "WebSocket配置类，定义了WebSocket消息代理的相关配置，包括STOMP端点和消息代理等")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    @Schema(description = "注册STOMP端点，允许跨域访问，并启用SockJS作为备用选项")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/analysis")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    @Schema(description = "配置消息代理，启用简单的内存消息代理，并设置应用程序目的地前缀")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}