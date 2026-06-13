package com.foodzie.websocket_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for the Pub/Sub backplane.
 *
 * Two beans:
 *   stringRedisTemplate — used by RedisLocationBackplane to PUBLISH messages
 *   redisMessageListenerContainer — manages all active Pub/Sub subscriptions
 *
 * The listener container is shared across all WebSocket sessions.
 * TrackingWebSocketHandler adds/removes channel subscriptions dynamically
 * as sessions connect and disconnect.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * Shared listener container for all Redis Pub/Sub subscriptions.
     * Subscriptions are added/removed dynamically per WebSocket session.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
