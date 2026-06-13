package com.foodzie.delivery_acceptance_service.config;

import com.foodzie.delivery_acceptance_service.service.RedisOfferExpiryListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires up the Redis keyspace notification listener.
 *
 * Subscribes to __keyevent@0__:expired so we receive a callback whenever
 * any key expires in Redis database 0.
 *
 * The RedisOfferExpiryListener filters for offer:pending:{orderId} keys only.
 *
 * IMPORTANT: Redis must have keyspace notifications enabled:
 *   redis-cli CONFIG SET notify-keyspace-events Ex
 *
 * In docker-compose, add to the redis service:
 *   command: redis-server --notify-keyspace-events Ex
 */
@Configuration
public class RedisKeyspaceConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisOfferExpiryListener expiryListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Listen for all key expiry events in database 0
        container.addMessageListener(expiryListener,
                new PatternTopic("__keyevent@0__:expired"));

        return container;
    }
}
