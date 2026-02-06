package com.winten.greenlight.core.db.repository.redis.ticket;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.ticket.Ticket;
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
public class TicketRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final JsonMapper jsonMapper;

    public Mono<Boolean> saveTicket(Ticket ticket, Duration ttl) {
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

    public Mono<Ticket> findTicketById(String ticketId) {
        String key = keyBuilder.ticket(ticketId);

        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> jsonMapper.readValue(json, Ticket.class)))
                .onErrorMap(e -> new IllegalArgumentException("Failed to deserialize Room. key=" + key, e));
    }
}