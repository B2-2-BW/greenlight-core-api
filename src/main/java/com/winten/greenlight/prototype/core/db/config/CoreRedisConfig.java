package com.winten.greenlight.prototype.core.db.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.core.db.repository.redis.event.EventEntity;
import com.winten.greenlight.prototype.core.domain.event.Event;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class CoreRedisConfig {

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties properties) {
        var config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        config.setPassword(properties.getPassword());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public ReactiveRedisOperations<String, String> reactiveRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }

    @Bean
    public ReactiveRedisTemplate<String, EventEntity> reactiveEventRedisTemplate(LettuceConnectionFactory factory, ObjectMapper objectMapper) {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<EventEntity> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, EventEntity.class);

        RedisSerializationContext<String, EventEntity> serializationContext = RedisSerializationContext
                .<String, EventEntity>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}