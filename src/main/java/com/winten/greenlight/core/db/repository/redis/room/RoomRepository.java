package com.winten.greenlight.core.db.repository.redis.room;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.room.RoomMetric;
import com.winten.greenlight.core.domain.ticket.Ticket;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoomRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveRedisTemplate<String, Object> jsonRedisTemplate;
    private final RoomRedisScript roomRedisScript;
    private final RedisKeyBuilder keyBuilder;
    private final JsonMapper jsonMapper;

    // TODO room이 없으면 notfound 로 떨구기. 500에러 떨어짐
    public Mono<Room> findRoomById(String roomId) {
        String key = keyBuilder.roomMeta(roomId);

        return redisTemplate.opsForValue().get(key)
                .map(json -> jsonMapper.readValue(json, Room.class))
                .onErrorMap(e -> new IllegalArgumentException("Failed to deserialize Room. key=" + key, e));
    }

    public Mono<Long> countWaitingCustomersInRoom(String roomId) {
        var key = keyBuilder.roomQueue(roomId, WaitStatus.WAITING);
        return redisTemplate.opsForZSet().size(key);
    }

    public Mono<RoomMetric> getLatestRoomMetric(String roomId) {
        var key = keyBuilder.roomMetricLatest(roomId);
        return redisTemplate.opsForValue().get(key)
                .map(value -> jsonMapper.readValue(value, RoomMetric.class));
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

    public Mono<Boolean> updateHeartbeatScore(String roomId, String ticketId, WaitStatus heartbeatType, long score) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        return redisTemplate.opsForZSet().add(key, ticketId, score);
    }

    public Mono<Long> deleteHeartbeat(String roomId, String ticketId, WaitStatus heartbeatType) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        return redisTemplate.opsForZSet().remove(key, ticketId);
    }

    public Mono<Long> increaseMetricCount(String roomId, WaitStatus metricType, long targetBucket) {
        var key = keyBuilder.roomMetricCounter(roomId, metricType, targetBucket);

        return redisTemplate.execute(
                roomRedisScript.getIncreaseMetricCountRedisScript(),
                List.of(key),
                List.of("180") //TTL은 3분
        ).next();
    }

    public Mono<Boolean> isSiteEnabled(String siteId) {
        String key = keyBuilder.siteInfoMeta(siteId);
        return jsonRedisTemplate.opsForHash().get(key, "siteEnabled")
                .map(obj -> Boolean.valueOf(obj.toString()));
    }

    public Mono<Long> deleteQueue(String roomId, String ticketId, WaitStatus waitStatus) {
        String key = keyBuilder.roomQueue(roomId, waitStatus);
        return redisTemplate.opsForZSet().remove(key, ticketId);
    }
}