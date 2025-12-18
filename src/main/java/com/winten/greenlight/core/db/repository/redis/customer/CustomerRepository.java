package com.winten.greenlight.core.db.repository.redis.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.core.domain.customer.CustomerSession;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepository {
    private final ReactiveRedisTemplate<String, String> stringRedisTemplate;
    private final ReactiveRedisTemplate<String, Object> jsonRedisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;

    public Mono<Boolean> enqueueCustomer(CustomerSession customerSession, WaitStatus waitStatus) {
        String key = keyBuilder.queue(customerSession.getActionGroupId(), waitStatus);
        return stringRedisTemplate.opsForZSet()
                .add(key, customerSession.getCustomerId(), customerSession.getTimestamp());
    }

    /**
     * 대기열에서 고객 삭제처리
     */
    public Mono<Boolean> deleteCustomer(Long actionGroupId, String customerId, WaitStatus waitStatus) {
        String key = keyBuilder.queue(actionGroupId, waitStatus);
        return stringRedisTemplate.opsForZSet().remove(key, customerId)
                .map(count -> count > 0);
    }

    public Mono<Boolean> isCustomerReady(Long actionGroupId, String customerId) {
        String key = keyBuilder.queue(actionGroupId, WaitStatus.READY);
        return stringRedisTemplate.opsForZSet().rank(key, customerId)
                .map(rank -> rank >= 0);
    }

    public Mono<Boolean> saveCustomerSession(CustomerSession session, Duration ttl) {
        String key = keyBuilder.customerSession(session.getCustomerId());
        Map<String, Object> map = objectMapper.convertValue(session, new TypeReference<>() {}); // DTO to Map 변환
        return jsonRedisTemplate.opsForHash().putAll(key, map)
                .then(jsonRedisTemplate.expire(key, ttl));
    }

    public Mono<CustomerSession> getCustomerSessionById(String customerId) {
        String key = keyBuilder.customerSession(customerId);
        return jsonRedisTemplate.opsForHash()
                .entries(key)
                .collectMap(entry -> (String) entry.getKey(), Map.Entry::getValue)
                .flatMap(map -> map.isEmpty()
                        ? Mono.error(CoreException.of(ErrorType.CUSTOMER_NOT_FOUND, "Customer session을 찾을 수 없습니다. customerId: " + customerId))
                        : Mono.just(objectMapper.convertValue(map, CustomerSession.class))
                );
    }

    public Mono<Boolean> findCustomerVerifiedFromSession(String customerId) {
        String key = keyBuilder.customerSession(customerId);
        return jsonRedisTemplate.opsForHash().get(key, "verified")
                .map(obj -> Boolean.valueOf(String.valueOf(obj)));
    }

    public Mono<Boolean> updateSessionVerified(String customerId, boolean verified) {
        String key = keyBuilder.customerSession(customerId);
        return jsonRedisTemplate.opsForHash().put(key, "verified", verified);
    }

    public Mono<Long> increaseSessionAccessCount(String customerId, Long amount) {
        String key = keyBuilder.customerSession(customerId);
        return jsonRedisTemplate.opsForHash().increment(key, "accessCount", 1L);
    }

    public Mono<Long> deleteCustomerSession(String customerId) {
        String key = keyBuilder.customerSession(customerId);
        return jsonRedisTemplate.opsForHash().remove(key, customerId);
    }
}