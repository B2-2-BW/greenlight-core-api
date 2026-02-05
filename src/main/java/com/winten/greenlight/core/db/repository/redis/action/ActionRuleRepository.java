
package com.winten.greenlight.core.db.repository.redis.action;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.core.domain.action.ActionRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ActionRule 데이터를 Redis에서 관리하는 Repository 클래스입니다.
 * Action ID를 키로, 관련된 모든 ActionRule들을 Redis의 Sorted Set에 저장하여 관리합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActionRuleRepository {

    private final ReactiveStringRedisTemplate redisTemplate;
    // Spring Boot가 자동으로 jsonMapper Bean을 주입해 줍니다.
    private final JsonMapper jsonMapper;

    private static final String ACTION_RULES_KEY_PREFIX = "action-rules:";

    /**
     * 특정 Action ID에 속한 모든 ActionRule들을 ruleSeq 순서대로 정렬하여 조회합니다.
     *
     * @param actionId 규칙을 조회할 Action의 ID
     * @return Flux<ActionRule> 조회된 ActionRule의 스트림
     */
    public Flux<ActionRule> findByActionId(Long actionId) {
        String key = ACTION_RULES_KEY_PREFIX + actionId;
        return redisTemplate.opsForZSet().range(key, Range.unbounded())
            .flatMap(this::deserializeToActionRule)
            .doOnError(e -> log.error("Failed to find or parse ActionRule for actionId: {}", actionId, e));
    }

    /**
     * 새로운 ActionRule을 Redis에 저장하거나, 이미 존재하면 점수(ruleSeq)를 업데이트합니다.
     *
     * @param rule 저장할 ActionRule 객체
     * @return Mono<Long> 저장 성공 시 1, 실패 시 0
     */
    public Mono<Boolean> save(ActionRule rule) {
        String key = ACTION_RULES_KEY_PREFIX + rule.getActionId();
        return serialize(rule)
            .flatMap(jsonRule ->
                redisTemplate.opsForZSet().add(key, jsonRule, rule.getRuleSeq().doubleValue())
            )
            .doOnError(e -> log.error("Failed to save ActionRule: {}", rule, e));
    }

    /**
     * ActionRule 객체를 JSON 문자열로 직렬화합니다.
     */
    private Mono<String> serialize(ActionRule rule) {
        try {
            return Mono.just(jsonMapper.writeValueAsString(rule));
        } catch (JacksonException e) {
            return Mono.error(new RuntimeException("Failed to serialize ActionRule", e));
        }
    }

    /**
     * JSON 문자열을 ActionRule 객체로 역직렬화합니다.
     */
    private Mono<ActionRule> deserializeToActionRule(String json) {
        try {
            return Mono.just(jsonMapper.readValue(json, ActionRule.class));
        } catch (JacksonException e) {
            return Mono.error(new RuntimeException("Failed to deserialize ActionRule from JSON", e));
        }
    }
}