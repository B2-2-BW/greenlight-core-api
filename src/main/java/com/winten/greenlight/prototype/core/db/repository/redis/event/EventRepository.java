package com.winten.greenlight.prototype.core.db.repository.redis.event;

import com.winten.greenlight.prototype.core.domain.event.Event;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class EventRepository {
    private final String KEY_PREFIX = "proto-event:";
    private final ReactiveRedisTemplate<String, EventEntity> redisTemplate;

    public EventRepository(ReactiveRedisTemplate<String, EventEntity> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Event> getEventByName(Event event) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + event.getEventName())
                .map(entity -> new Event(entity))
                .onErrorMap(e -> new CoreException(ErrorType.REDIS_ERROR, e.getMessage()));
    }
}