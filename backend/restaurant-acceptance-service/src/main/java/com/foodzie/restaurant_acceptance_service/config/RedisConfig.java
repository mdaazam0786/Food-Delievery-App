package com.foodzie.restaurant_acceptance_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.foodzie.restaurant_acceptance_service.model.PendingOrder;
import com.foodzie.restaurant_acceptance_service.service.RedisExpiryListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Typed template for PendingOrder objects.
     * Keys are plain strings; values are JSON via Jackson.
     */
    @Bean
    public RedisTemplate<String, PendingOrder> pendingOrderRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<PendingOrder> valueSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, PendingOrder.class);

        RedisTemplate<String, PendingOrder> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);
        return template;
    }

    /**
     * Listens for Redis keyspace expiry events on all keys.
     * RedisExpiryListener filters for "pending_order:*" keys internally.
     *
     * Requires Redis keyspace notifications enabled:
     *   redis-cli CONFIG SET notify-keyspace-events Ex
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisExpiryListener expiryListener) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(expiryListener, "onKeyExpired");

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(adapter, new PatternTopic("__keyevent@*__:expired"));
        return container;
    }
}
