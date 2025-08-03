package com.winten.greenlight.prototype.core.support.publisher;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionEventPublisher {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final RedisKeyBuilder keyBuilder;

    public Mono<RecordId> publish(Customer customer) {
        return this.publish(
                customer.getWaitStatus(),
                customer.getActionGroupId(),
                customer.getActionId(),
                customer.getCustomerId(),
                System.currentTimeMillis()
        );
    }

    public Mono<RecordId> publish(ActionEvent actionEvent) {
        return this.publish(
            actionEvent.getWaitStatus(),
            actionEvent.getActionGroupId(),
            actionEvent.getActionId(),
            actionEvent.getCustomerId(),
            System.currentTimeMillis()
        );
    }
    /**
     * 공통 이벤트 발행 로직
     */
    public Mono<RecordId> publish(WaitStatus waitStatus, Long actionGroupId, Long actionId, String customerId, Long timestamp) {
        // InfluxDB의 Tag에 해당하는 데이터를 Map으로 구성합니다.
        // Consumer가 이 데이터를 읽어 InfluxDB에 저장하게 됩니다.
        var eventPayload = Map.of(
                "eventType", waitStatus.name(),
                "actionGroupId", actionGroupId,
                "actionId", actionId,
                "customerId", customerId,
                "timestamp", timestamp
        );

        // Redis Stream에 추가할 레코드를 생성합니다.
        var record = MapRecord.create(keyBuilder.actionEventStream(), eventPayload);
        // opsForStream().add()를 사용하여 비동기적으로 스트림에 데이터를 추가합니다.
        return redisTemplate.opsForStream()
                .add(record)
                .doOnError(e -> log.error("Failed to publish event to Redis Stream", e));
    }
}