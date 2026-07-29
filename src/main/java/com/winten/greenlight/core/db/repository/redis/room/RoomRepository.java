package com.winten.greenlight.core.db.repository.redis.room;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.room.RoomMetric;
import com.winten.greenlight.core.domain.site.SiteOperationStatus;
import com.winten.greenlight.core.domain.ticket.Ticket;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveZSetCommands;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.util.ByteUtils;
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

    public Mono<Boolean> createHeartbeatScore(String roomId, String ticketId, WaitStatus heartbeatType, long score) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        return redisTemplate.opsForZSet().add(key, ticketId, score);
    }

    public Mono<Boolean> refreshHeartbeatScore(String roomId, String ticketId, WaitStatus heartbeatType, long score) {
        String key = keyBuilder.roomHeartbeat(roomId, heartbeatType);
        var serializationContext = redisTemplate.getSerializationContext();
        var rawKey = serializationContext.getKeySerializationPair().write(key);
        var rawTicketId = serializationContext.getValueSerializationPair().write(ticketId);
        var tuple = new DefaultTuple(ByteUtils.getBytes(rawTicketId), (double) score);
        var command = ReactiveZSetCommands.ZAddCommand.tuple(tuple)
                .to(rawKey)
                .xx()
                .ch();

        return redisTemplate.execute(connection ->
                        connection.zSetCommands()
                                .zAdd(Mono.just(command))
                                .map(response -> response.getOutput().longValue() > 0)
                )
                .next();
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

    public Mono<SiteOperationStatus> findSiteOperationStatus(String siteId) {
        String key = keyBuilder.siteInfoMeta(siteId);
        return jsonRedisTemplate.<String, Object>opsForHash()
                .multiGet(key, List.of("siteEnabled", "queueEnabled"))
                .map(this::toSiteOperationStatus);
    }

    private SiteOperationStatus toSiteOperationStatus(List<Object> values) {
        Object siteEnabled = values.get(0);
        Object queueEnabled = values.get(1);

        if (queueEnabled == null) { // Legacy: siteEnabled represented queue operation status.
            return new SiteOperationStatus(true, toBoolean(siteEnabled));
        }
        return new SiteOperationStatus(toBoolean(siteEnabled), toBoolean(queueEnabled));
    }

    private boolean toBoolean(Object value) {
        return value != null && Boolean.parseBoolean(value.toString());
    }

    public Mono<Long> deleteQueue(String roomId, String ticketId, WaitStatus waitStatus) {
        String key = keyBuilder.roomQueue(roomId, waitStatus);
        return redisTemplate.opsForZSet().remove(key, ticketId);
    }
}
