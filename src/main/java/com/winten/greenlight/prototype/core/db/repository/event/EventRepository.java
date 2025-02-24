package com.winten.greenlight.prototype.core.db.repository.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.core.db.repository.customer.EventStringEntity;
import com.winten.greenlight.prototype.core.domain.event.Event;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class EventRepository {
    private final String KEY_PREFIX = "proto-event:";
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public EventRepository(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public Mono<Event> getEvent(Event event) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + event.getEventId())
                .doOnError(e -> {
                    System.out.println(e);
                })
                .onErrorResume(e -> {
                    System.out.println(e);
                    return Mono.error(new CoreException(ErrorType.REDIS_ERROR, e.getMessage()));
                })
                .flatMap(json -> {
                    try {
                        EventStringEntity eventEntity = objectMapper.readValue(json, EventStringEntity.class);
                        return Mono.just(new Event(eventEntity));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new CoreException(ErrorType.INVALID_DATA, e.getMessage()));
                    }
                });
    }
}