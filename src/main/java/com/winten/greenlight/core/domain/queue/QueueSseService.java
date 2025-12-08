package com.winten.greenlight.core.domain.queue;

import com.winten.greenlight.core.domain.action.ActionService;
import com.winten.greenlight.core.domain.action.CachedActionService;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class QueueSseService {
    private final ActionService actionService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    //사용자별로 상태를 push할 수 있는 sink 저장소
    // key: actionGroupId:customerId ( 마음대로 정할 수 있지만, 보통 해당 형식으로 설정 )
    private final Map<String, Sinks.Many<CustomerQueueInfo>> userSinkMap = new ConcurrentHashMap<>();
    private final CachedActionService cachedActionService;

    // 참고자료: Flux.interval 사용 시 모든 사용자 요청에 interval 이 생성됨
    // 사용자 요청은 연결만 진행하고 일정 주기로 전체 사용자에게 일괄로 상태를 전송하는 방식으로 구현 필요
    // https://happyzodiac.tistory.com/90
    // https://why-doing.tistory.com/136

    @PostConstruct
    //서비스 시작 시 단일 interval 생성, 모든 접속 사용자에게 상태 정보를 동시에 push
    public void startBroadcastLoop() {
        Flux.interval(Duration.ofSeconds(1))  // 1초마다 주기적으로 실행
                .onBackpressureDrop()        // backpressure 시 오래된 tick drop
                .subscribe(tick -> broadcastStatuses());
    }

    //사용자 식별을 위한 고유 키 생성 함수
    private String generateKey(Long actionGroupId, String customerId) {
        return actionGroupId + ":" + customerId;
    }

    // 사용자 SSE 연결 시 호출되는 메소드
    // Sinks.Many를 생성해 저장 후 Flux로 반환 / sse 연결 종료 시 Map에서 제거
    public Flux<CustomerQueueInfo> subscribe(Long actionGroupId, String customerId) {
        String key = generateKey(actionGroupId, customerId);

        //Sink 생성 ( 마지막 이벤트만 재전송하는 replay 최신 방식 )
        //Sink가 마지막으로 emit한 값을 기억 -> 새로 구독한 클라이언트에게도 마지막 값 전달

        //Sink.many() 여러값을 발행할 수 있는 Sink를 생성하는 진입점 -> 많은 값을 여러번 보낼 수 있는 스트림을 만들겠다
        Sinks.Many<CustomerQueueInfo> sink = Sinks.many().replay().latest();

        //Sink 저장 ( 고객별 key로 식별 )
        userSinkMap.put(key, sink);

        //연결된 Flux 반환 ( 끊길 경우 자동 제거 )
        return sink.asFlux()
                .doFinally(signalType -> userSinkMap.remove(key));
    }

    //모든 접속 사용자에 대해 대기열 상태 조회 후 push
    private void broadcastStatuses() {
        for (Map.Entry<String, Sinks.Many<CustomerQueueInfo>> entry : userSinkMap.entrySet()) {
            String key = entry.getKey();
            Sinks.Many<CustomerQueueInfo> sink = entry.getValue();

            // key = actionGroupId:customerId
            String[] parts = key.split(":");
            Long actionGroupId = Long.valueOf(parts[0]);
            String customerId = parts[1];

            // 고객의 현재 상태 조회 후 Sink에 push
            findUserQueueInfo(actionGroupId, customerId)
                    .subscribe(sink::tryEmitNext);
        }
    }

    // sse 연결해서 고객의 현재 대기상태를 조회할 때 사용
    public Flux<CustomerQueueInfo> connect(Long actionGroupId, String customerId) {
        return Flux.interval(Duration.ofSeconds(2))   // 2초마다 polling
                .flatMap(tick -> findUserQueueInfo(actionGroupId, customerId))
                .distinctUntilChanged();             // 상태가 바뀔 때만 이벤트 발행
    }

    public Mono<CustomerQueueInfo> findUserQueueInfo(Long actionGroupId, String customerId) {
        String waitingKey = redisKeyBuilder.queue(actionGroupId, WaitStatus.WAITING);
        return Mono.zip(redisTemplate.opsForZSet().rank(waitingKey, customerId),
                        redisTemplate.opsForZSet().size(waitingKey)
                )
                // rank 및 size가 Waiting Queue에 있다면 대기중
                .flatMap(tuple -> actionService.getActionGroupById(actionGroupId)
                                    .map(actionGroup -> {
                                        Long estimatedWaitTime = actionGroup.getMaxTrafficPerSecond() > 0  //  = 대기 position / 최대활성사용자수, 나누기 0 방어로직 추가
                                                ? Math.round((double) tuple.getT1() / actionGroup.getMaxTrafficPerSecond())
                                                : -1L;
                                        return CustomerQueueInfo.builder()
                                                .customerId(customerId)
                                                .estimatedWaitTime(estimatedWaitTime)
                                                .aheadCount(Math.max(tuple.getT1(), 0))
                                                .behindCount(Math.max(tuple.getT2() - (tuple.getT1() + 1), 0))
                                                .position(tuple.getT1() + 1) // Redis rank는 0-based → +1
                                                .waitStatus(WaitStatus.WAITING)
                                                .build();
                                    })
                )
                // rank 및 size가 Ready Queue에 있다면 WaitStatus만 반환
                .switchIfEmpty(redisTemplate.opsForZSet().rank(redisKeyBuilder.queue(actionGroupId, WaitStatus.READY), customerId)
                        .map(rank -> CustomerQueueInfo.builder()
                                    .customerId(customerId)
                                    .waitStatus(WaitStatus.READY)
                                    .estimatedWaitTime(0L)
                                    .queueSize(0L)
                                    .position(0L)
                                    .build()
                        ))
                // Waiting 과 Ready Queue에서 전부 찾을 수 없었다면
                .switchIfEmpty(Mono.error(new CoreException(ErrorType.CUSTOMER_NOT_FOUND, "이미 입장했거나 존재하지 않는 고객 ID입니다: " + customerId)));
    }

}