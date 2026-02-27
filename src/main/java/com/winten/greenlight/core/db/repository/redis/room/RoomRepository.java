package com.winten.greenlight.core.db.repository.redis.room;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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

    public Mono<Long> countCustomersInRoomQueue(String roomId, WaitStatus waitStatus) {
        var key = keyBuilder.roomQueue(roomId, waitStatus);
        return redisTemplate.opsForZSet().size(key);
    }

    public Mono<Long> add(String key, String value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score)
                .flatMap(added -> added ? redisTemplate.opsForZSet().rank(key, value) : Mono.just(-1L)); // 추가 성공 시 순위 반환, 실패 시 -1L
    }
}