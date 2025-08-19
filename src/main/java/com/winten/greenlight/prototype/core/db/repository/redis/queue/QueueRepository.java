package com.winten.greenlight.prototype.core.db.repository.redis.queue;

import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 대기열 관련 Redis 작업을 수행하는 Repository 클래스입니다.
 * Redis Sorted Set (ZSET)을 사용하여 대기열 및 활성 사용자 수를 관리합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;

    /**
     * 특정 ActionGroup에 진입 가능한 수를 조회합니다.
     *
     * @param actionGroupId 조회할 ActionGroup의 ID
     * @return Mono<Long> ActionGroup 내 진입 가능한 수
     */
    public Mono<Long> getAvailableCapacity(Long actionGroupId) {
        String key = keyBuilder.actionGroupStatus(actionGroupId);
        return redisTemplate.opsForHash().entries(key)
                .collectMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue())
                )
                .map(map ->{
                            long waitingQueueSize = Long.parseLong(map.getOrDefault("waitingQueueSize", "0"));
                            long availableCapacity = Long.parseLong(map.getOrDefault("availableCapacity", "0"));
                            return (waitingQueueSize == 0L && availableCapacity > 0L) ? availableCapacity : 0L;
                })
                .switchIfEmpty(Mono.just(0L))
                .onErrorResume(e -> {
                    log.error("failed to get available capacity", e);
                    return Mono.just(0L);
                }); // ZSET의 요소 개수 반환
    }

    /**
     * 지정된 키의 Sorted Set에 사용자를 추가합니다.
     * Redis ZSET의 ADD 명령어를 사용하며, 현재 시간을 score로 사용하여 순서를 결정합니다.
     *
     * @param key      Redis Sorted Set의 키
     * @param value    추가할 사용자 ID
     * @param score    순서를 결정하는 점수
     * @return Mono<Long> 추가된 후의 순번 (0부터 시작)
     */
    public Mono<Long> add(String key, String value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score)
            .flatMap(added -> added ? redisTemplate.opsForZSet().rank(key, value) : Mono.just(-1L)); // 추가 성공 시 순위 반환, 실패 시 -1L
    }

    /**
     * 대기열에서 특정 사용자의 순번을 조회합니다.
     *
     * @param actionId 조회할 Action의 ID
     * @param queueId  조회할 사용자의 고유 대기 ID
     * @return Mono<Long> 대기 순번 (0부터 시작)
     */
    public Mono<Long> getRankFromWaitingQueue(Long actionId, String queueId) {
        String key = keyBuilder.waitingQueue(actionId);
        return redisTemplate.opsForZSet().rank(key, queueId);
    }

    public Mono<Double> getCurrentRequestPerSec(Long actionGroupId) {
        String key = keyBuilder.requestLog(actionGroupId);
        return redisTemplate.opsForZSet().size(key)
                .map(val -> val / 10.0);
    }
}