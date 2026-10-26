package com.winten.greenlight.core.domain.scheduler;

import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SchedulerRunningStatusReader {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public Mono<Boolean> isEnabled(SchedulerCode schedulerCode) {
        return redisTemplate.opsForValue().get(redisKeyBuilder.schedulerEnabled(schedulerCode.name()))
                .map(Boolean::parseBoolean)
                .defaultIfEmpty(true);
    }
}
