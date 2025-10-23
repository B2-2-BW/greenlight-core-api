package com.winten.greenlight.core.support.logging;

import com.winten.greenlight.core.support.error.LogLevel;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class LogManager {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String LOG_STREAM_KEY = "admin:log:stream";

    public LogManager(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Log> push(Log log) {
        return redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .ofMap(log.toMap())
                        .withStreamKey(LOG_STREAM_KEY)
                )
                .thenReturn(log);
    }

    public Mono<Log> push(LogLevel logLevel, final String message, final SystemType systemType, final String userId, final String userIp) {
        Log log = new Log(LocalDateTime.now(), logLevel.name(), message, systemType, userId, userIp);
        return push(log);
    }

    public Mono<Log> info(final String message, final SystemType systemType, final String userId, final String userIp) {
        return push(LogLevel.INFO, message, systemType, userId, userIp);
    }
    public Mono<Log> warn(final String message, final SystemType systemType, final String userId, final String userIp) {
        return push(LogLevel.WARN, message, systemType, userId, userIp);
    }
    public Mono<Log> error(final String message, final SystemType systemType, final String userId, final String userIp) {
        return push(LogLevel.WARN, message, systemType, userId, userIp);
    }
}