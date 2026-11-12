package com.foodzie.delivery_matching_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration with explicit serialization.
 * 
 * Connection pooling and timeouts are configured via:
 * - spring.redis.lettuce.pool.* properties
 * - spring.data.redis.timeout property
 * 
 * Used for:
 * - Driver geospatial data (GEOSEARCH)
 * - Driver heartbeat tracking
 * - Real-time driver location updates
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate for string operations.
     * Handles geospatial commands for driver location tracking.
     * Spring auto-configures the RedisConnectionFactory, so we just configure the template.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use StringRedisSerializer for both keys and values
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);
        
        template.afterPropertiesSet();
        log.info("RedisTemplate configured with StringRedisSerializer");
        
        return template;
    }
}
