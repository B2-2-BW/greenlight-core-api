package com.winten.greenlight.core.db.repository.redis.ticket;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.ticket.Ticket;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TicketRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final JsonMapper jsonMapper;

    public Mono<Boolean> saveTicket(TicketEntity ticket, Duration ttl) {
        String key = keyBuilder.ticket(ticket.getTicketId());

        return Mono.fromCallable(() -> jsonMapper.writeValueAsString(ticket))
                .flatMap(json ->
                        redisTemplate.opsForValue().set(key, json, ttl)
                );
    }

    public Mono<Long> findQueuePosition(String roomId, String ticketId, WaitStatus waitStatus) {
        String key = keyBuilder.roomQueue(roomId, waitStatus);
        return redisTemplate.opsForZSet().rank(key, ticketId);
    }

    public Mono<Long> findHeartbeatPosition(String roomId, String ticketId, WaitStatus waitStatus) {
        String key = keyBuilder.roomHeartbeat(roomId, waitStatus);
        return redisTemplate.opsForZSet().rank(key, ticketId);
    }

    // TODO ticket이 없으면 notfound 로 떨구기. 500에러 떨어짐
    public Mono<Ticket> findTicketById(String ticketId) {
        String key = keyBuilder.ticket(ticketId);

        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> jsonMapper.readValue(json, Ticket.class)))
                .onErrorMap(e -> new IllegalArgumentException("Failed to deserialize Ticket. key=" + key, e));
    }
}