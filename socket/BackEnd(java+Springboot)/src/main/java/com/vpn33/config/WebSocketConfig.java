package com.vpn33.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 本机的websocket的配置
 */
@Configuration
public class WebSocketConfig {
    /**
     * ”@ServerEndpoint“方式的声明 不加的话无法mapping接口 直接连接会报404
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
