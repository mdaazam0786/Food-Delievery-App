package com.foodzie.delivery_matching_service.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration with explicit connection pooling and timeouts.
 * 
 * Critical for performance:
 * - Connection pool prevents exhaustion under load
 * - Timeouts prevent indefinite blocking on slow Redis
 * - Proper serializers ensure type safety
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
     * Customizes Lettuce client options with timeouts and socket settings.
     * Called automatically by Spring when creating LettuceConnectionFactory.
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return clientConfigBuilder -> {
            clientConfigBuilder.clientOptions(ClientOptions.builder()
                    .socketOptions(SocketOptions.builder()
                            // Connection timeout: fail fast if Redis unreachable
                            .connectTimeout(Duration.ofSeconds(3))
                            .keepAlive(true)  // Enable TCP keepalive
                            .build())
                    // Disconnect timeout on shutdown
                    .disconnectTimeout(Duration.ofSeconds(2))
                    // Command timeout: prevent indefinite blocking on slow Redis
                    .commandTimeout(Duration.ofSeconds(5))
                    .build());
            
            log.info("Redis Lettuce client configured with 3s connect timeout, 5s command timeout");
        };
    }

    /**
     * Redis connection factory with connection pooling.
     * Lettuce uses Apache Commons Pool for connection pooling.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
        
        // Connection pool configuration
        // minIdle: minimum idle connections to maintain
        // maxIdle: maximum idle connections allowed
        // maxActive: maximum total connections
        // maxWait: max time to wait for connection from pool
        connectionFactory.getPoolConfig()
                .setMinIdle(5);
        connectionFactory.getPoolConfig()
                .setMaxIdle(20);
        connectionFactory.getPoolConfig()
                .setMaxTotal(50);
        connectionFactory.getPoolConfig()
                .setMaxWaitMillis(5000);
        
        // Test connections in pool
        connectionFactory.getPoolConfig()
                .setTestOnBorrow(true);
        connectionFactory.getPoolConfig()
                .setTestOnReturn(true);
        connectionFactory.getPoolConfig()
                .setTestWhileIdle(true);
        
        // Evict idle connections every 10 minutes
        connectionFactory.getPoolConfig()
                .setTimeBetweenEvictionRunsMillis(600000);
        connectionFactory.getPoolConfig()
                .setMinEvictableIdleTimeMillis(300000);  // 5 minutes
        
        log.info("Redis connection pool configured: minIdle=5, maxIdle=20, maxTotal=50");
        
        return connectionFactory;
    }

    /**
     * RedisTemplate for string operations.
     * Handles geospatial commands for driver location tracking.
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
