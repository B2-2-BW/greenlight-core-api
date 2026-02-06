package com.winten.greenlight.core.db.config;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public ReactiveRedisOperations<String, String> reactiveRedisTemplate(LettuceConnectionFactory factory) {
        var serializer = new StringRedisSerializer();

        var serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveJsonRedisTemplate(LettuceConnectionFactory factory, JsonMapper jsonMapper) {
        var keySerializer = new StringRedisSerializer();
        var jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        var serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(jsonSerializer)
                .hashKey(keySerializer)
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}