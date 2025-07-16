package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class QueueSseService {
    private final QueueRepository queueRepository;
    private final RedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate redisTemplate;

    // 참고자료: Flux.interval 사용 시 모든 사용자 요청에 interval 이 생성됨
    // 사용자 요청은 연결만 진행하고 일정 주기로 전체 사용자에게 일괄로 상태를 전송하는 방식으로 구현 필요
    // https://happyzodiac.tistory.com/90
    // https://why-doing.tistory.com/136

    // sse 연결해서 고객의 현재 대기상태를 조회할 때 사용
    public Flux<WaitStatus> connect(Long actionGroupId, String entryId) {
        return Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> findUserStatus(actionGroupId, entryId))
                .distinctUntilChanged();
    }

    public void updateQueueStatus() {
    }

    public Mono<WaitStatus> findUserStatus(Long actionGroupId, String entryId) {
        for (WaitStatus status : WaitStatus.values()) {
            String redisKey = redisKeyBuilder.queue(actionGroupId, status);
            Long rank = redisTemplate.opsForZSet().rank(redisKey, entryId);
            if (rank != null) {
                return Mono.just(status);
            }
        }
        return Mono.empty();
    }

}