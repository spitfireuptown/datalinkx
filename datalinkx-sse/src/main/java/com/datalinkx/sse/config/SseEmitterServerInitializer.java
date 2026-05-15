package com.datalinkx.sse.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
public class SseEmitterServerInitializer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @PostConstruct
    public void init() {
        try {
            String nodeId = generateNodeId();
            SseEmitterServer.setRedisTemplate(stringRedisTemplate);
            SseEmitterServer.setListenerContainer(createListenerContainer());
            SseEmitterServer.setCurrentNodeId(nodeId);
            log.info("SSE Emitter Server initialized with nodeId: {}", nodeId);
        } catch (Exception e) {
            log.error("Failed to initialize SSE Emitter Server: {}", e.getMessage());
        }
    }

    private RedisMessageListenerContainer createListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    private String generateNodeId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "node-unknown";
        }
    }
}