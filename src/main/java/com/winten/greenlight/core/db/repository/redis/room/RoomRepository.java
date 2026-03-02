package com.winten.greenlight.core.db.repository.redis.room;

import com.winten.greenlight.core.domain.ticket.Ticket;
import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoomRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final JsonMapper jsonMapper;

    // TODO room이 없으면 notfound 로 떨구기. 500에러 떨어짐
    public Mono<Room> findRoomById(String roomId) {
        String key = keyBuilder.roomMeta(roomId);

        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> jsonMapper.readValue(json, Room.class)))
                .onErrorMap(e -> new IllegalArgumentException("Failed to deserialize Room. key=" + key, e));
    }

    public Mono<Long> countWaitingCustomersInRoom(String roomId) {
        var key = keyBuilder.roomQueue(roomId, WaitStatus.WAITING);
        return redisTemplate.opsForZSet().size(key);
    }

    public Mono<Long> countEnteredCustomersInRoom(String roomId) {
        var key = keyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED);
        return redisTemplate.opsForZSet().size(key);
    }

    public Mono<Long> addToRoomQueue(Ticket ticket, WaitStatus waitStatus) {
        String key = keyBuilder.roomQueue(ticket.getRoomId(), waitStatus);
        return redisTemplate.opsForZSet().add(key, ticket.getTicketId(), ticket.getTimestamp())
                .flatMap(_ -> redisTemplate.opsForZSet().rank(key, ticket.getTicketId()));
    }

    public Mono<Boolean> addToRoomInflow(Ticket ticket) {
        String key = keyBuilder.roomMetricInflow(ticket.getRoomId());
        // 동일한 사용자를 위해 같은 key id를 쓸 경우 유입량 계산에 오류가 발생할 수 있음. 따라서 ticketId + timestamp 조합으로 value를 기록 (ex 새로고침 시)
        return redisTemplate.opsForZSet().add(key, ticket.getTicketId() + ":" + ticket.getTimestamp(), ticket.getTimestamp());
    }

    public Mono<Boolean> updateHeartbeatScore(String roomId, String ticketId, WaitStatus heartbeatType) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        return redisTemplate.opsForZSet().add(key, ticketId, System.currentTimeMillis());
    }

    public Mono<Long> deleteHeartbeat(String roomId, String ticketId, WaitStatus heartbeatType) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        return redisTemplate.opsForZSet().remove(key, ticketId);
    }

    public Mono<Long> increaseMetricCount(String roomId, WaitStatus metricType, long targetBucket) {
        String key = keyBuilder.roomMetricCounter(roomId, metricType, targetBucket);
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                if (count == 1L) { // count == 1 이면 처음 만들어진 키이므로 TTL 세팅
                    return redisTemplate.expire(key, Duration.ofSeconds(30))
                            .thenReturn(count);
                }
                return Mono.just(count);
        });
    }
}