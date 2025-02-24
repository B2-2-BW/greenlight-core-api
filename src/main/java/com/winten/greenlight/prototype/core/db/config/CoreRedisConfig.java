package com.winten.greenlight.prototype.core.db.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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


}