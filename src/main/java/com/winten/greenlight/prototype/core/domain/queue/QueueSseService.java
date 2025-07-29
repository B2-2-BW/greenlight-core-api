package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class QueueSseService {
    private final QueueRepository queueRepository;
    private final RedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate redisTemplate;

    //사용자별로 상태를 push할 수 있는 sink 저장소
    // key: actionGroupId:customerId ( 마음대로 정할 수 있지만, 보통 해당 형식으로 설정 )
    private final Map<String, Sinks.Many<WaitStatus>> userSinkMap = new ConcurrentHashMap<>();

    // 참고자료: Flux.interval 사용 시 모든 사용자 요청에 interval 이 생성됨
    // 사용자 요청은 연결만 진행하고 일정 주기로 전체 사용자에게 일괄로 상태를 전송하는 방식으로 구현 필요
    // https://happyzodiac.tistory.com/90
    // https://why-doing.tistory.com/136

    @PostConstruct
    //서비스 시작 시 단일 interval 생성, 모든 접속 사용자에게 상태 정보를 동시에 push
    public void startBroadcastLoop() {
        Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .subscribe(tick -> broadcastStatuses());
    }

    //사용자 식별을 위한 고유 키 생성 함수
    private String generateKey(Long actionGroupId, String customerId) {
        return actionGroupId + ":" + customerId;
    }

    // 사용자 SSE 연결 시 호출되는 메소드
    // Sinks.Many를 생성해 저장 후 Flux로 반환 / sse 연결 종료 시 Map에서 제거
    public Flux<WaitStatus> subscribe(Long actionGroupId, String customerId) {
        String key = generateKey(actionGroupId, customerId);

        //Sink 생성 ( 마지막 이벤트만 재전송하는 replay 최신 방식 )
        //Sink가 마지막으로 emit한 값을 기억 -> 새로 구독한 클라이언트에게도 마지막 값 전달

        //Sink.many() 여러값을 발행할 수 있는 Sink를 생성하는 진입점 -> 많은 값을 여러번 보낼 수 있는 스트림을 만들겠다
        Sinks.Many<WaitStatus> sink = Sinks.many().replay().latest();

        //Sink 저장 ( 고객별 key로 식별 )
        userSinkMap.put(key, sink);

        //연결된 Flux 반환 ( 끊길 경우 자동 제거 )
        return sink.asFlux()
                .doFinally(signalType -> userSinkMap.remove(key));
    }

    //모든 접속 사용자에 대해 대기열 상태 조회 후 push
    private void broadcastStatuses() {
        for (Map.Entry<String, Sinks.Many<WaitStatus>> entry : userSinkMap.entrySet()) {
            String key = entry.getKey();
            Sinks.Many<WaitStatus> sink = entry.getValue();

            // key = actionGroupId:entryId
            String[] parts = key.split(":");
            Long actionGroupId = Long.valueOf(parts[0]);
            String entryId = parts[1];

            //현재 사용자의 대기 상태 조회 후 sink로 전달
            findUserStatus(actionGroupId, entryId).subscribe(sink::tryEmitNext);
        }
    }

    // sse 연결해서 고객의 현재 대기상태를 조회할 때 사용
    public Flux<WaitStatus> connect(Long actionGroupId, String entryId) {
        return Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> findUserStatus(actionGroupId, entryId))
                .distinctUntilChanged();
    }

    public void updateQueueStatus() {
    }

    public Mono<WaitStatus> findUserStatus(Long actionGroupId, String customerId) {
        List<WaitStatus> targetStatuses = List.of(WaitStatus.READY, WaitStatus.WAITING);

        for (WaitStatus status : targetStatuses) {
            String redisKey = redisKeyBuilder.queue(actionGroupId, status);
            Long rank = redisTemplate.opsForZSet().rank(redisKey, customerId);
            if (rank != null) {
                return Mono.just(status);
            }
        }
        return Mono.empty();
    }

}
