package com.winten.greenlight.core.db.repository.redis.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.core.domain.action.Action;
import com.winten.greenlight.core.domain.action.ActionGroup;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ActionRepository {
    private final ReactiveRedisTemplate<String, String> stringRedisTemplate;
    private final ReactiveRedisTemplate<String, Object> jsonRedisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;

    private static final String ACTION_KEY_PREFIX = "action:url:";

    public Mono<Action> findByUrl(String url) {
        String key = ACTION_KEY_PREFIX + url;
        return jsonRedisTemplate.opsForValue().get(key)
            .flatMap(json -> deserializeToAction(String.valueOf(json))) // <-- 메소드 참조 대신 람다 표현식 사용
            .doOnError(e -> log.error("Failed to find or parse Action for url: {}", url, e));
    }

    public Mono<Action> getActionById(Long actionId) {
        String key = keyBuilder.action(actionId);

        return jsonRedisTemplate.opsForHash().entries(key)
                .collectMap(entry -> (String) entry.getKey(), Map.Entry::getValue)
                .flatMap(map -> map.isEmpty()
                        ? Mono.error(CoreException.of(ErrorType.ACTION_NOT_FOUND, "Action을 찾을 수 없습니다. actionId: " + actionId))
                        : Mono.just(objectMapper.convertValue(map, Action.class))
                );
    }

    public Mono<ActionGroup> getActionGroupById(Long actionGroupId) {
        String key = keyBuilder.actionGroupMeta(actionGroupId);
        return jsonRedisTemplate.opsForHash().entries(key)
                .collectMap(entry -> (String) entry.getKey(), Map.Entry::getValue)
                .flatMap(map -> map.isEmpty()
                        ? Mono.error(CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "Action Group을 찾을 수 없습니다. actionGroupId: " + actionGroupId))
                        : Mono.just(objectMapper.convertValue(map, ActionGroup.class))
                );
    }

    private Mono<Action> deserializeToAction(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, Action.class));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to deserialize Action from JSON", e));
        }
    }

    public Mono<String> getUserApiKey() {
        return stringRedisTemplate.opsForValue().get(keyBuilder.userApiKey());

    }

    public Flux<Action> getAllActions() {
        return stringRedisTemplate.keys(keyBuilder.allActions())
                .flatMap(key -> jsonRedisTemplate.opsForHash().entries(key)
                        .collectMap(entry -> (String) entry.getKey(), Map.Entry::getValue)
                )
                .map(map -> objectMapper.convertValue(map, Action.class));
    }

    public Mono<String> getActionIdByLandingId(String landingId) {
        var key = keyBuilder.landingCacheKey(landingId);
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> putRequestLog(Long actionGroupId, String customerId) {
        var key = keyBuilder.actionGroupRequestLog(actionGroupId);
        var time = System.currentTimeMillis();
        return stringRedisTemplate.opsForZSet().add(key, customerId + time, time);
    }

    public Mono<Boolean> putAccessLog(Long actionGroupId, String customerId) {
        var key = keyBuilder.actionGroupAccessLog(actionGroupId);
        var time = System.currentTimeMillis();
        return stringRedisTemplate.opsForZSet().add(key, customerId + time, time);
    }

    public Mono<Long> getWaitingCountByActionGroupId(Long actionGroupId) {
        var key = keyBuilder.queue(actionGroupId, WaitStatus.WAITING);
        return stringRedisTemplate.opsForZSet().size(key);
    }

    public Mono<Boolean> putSession(String uniqueId) {
        var key = keyBuilder.actionGroupSession();
        return stringRedisTemplate.opsForZSet().add(key, uniqueId, System.currentTimeMillis());
    }
}