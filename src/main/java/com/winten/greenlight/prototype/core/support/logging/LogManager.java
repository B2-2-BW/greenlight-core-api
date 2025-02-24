package com.winten.greenlight.prototype.core.support.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LogManager {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String LOG_QUEUE_KEY = "system:log:queue";

    public LogManager(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Log> push(Log log) {
        String logJson = convertLoggingToJson(log);
        return redisTemplate.opsForList()
                .leftPush(LOG_QUEUE_KEY, logJson)
                .thenReturn(log);
    }

    private String convertLoggingToJson(Log log) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(log);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Logging to JSON", e);
        }
    }
}