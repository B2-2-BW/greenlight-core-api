package com.winten.greenlight.core.db.repository.redis.customer;

import com.winten.greenlight.core.domain.customer.Customer;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.support.util.CustomerUtil;
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
public class CustomerRepository {
    private final ReactiveRedisTemplate<String, String> stringRedisTemplate;
    private final RedisKeyBuilder keyBuilder;

    public Mono<Customer> enqueueCustomer(Customer customer, WaitStatus waitStatus) {
        String key = keyBuilder.queue(customer.getActionGroupId(), waitStatus);
        return stringRedisTemplate.opsForZSet()
                .add(key, customer.getCustomerId(), customer.getScore())
                .flatMap(result -> Mono.just(customer));
    }

    /**
     * 대기열에서 고객 삭제처리
     * @param customer actionGroupId 및 customerId 필수
     * @param waitStatus
     * @return
     */
    public Mono<Long> deleteCustomer(Customer customer, WaitStatus waitStatus) {
        String key = keyBuilder.queue(customer.getActionGroupId(), waitStatus);
        return stringRedisTemplate.opsForZSet().remove(key, customer.getCustomerId());
    }

    public Mono<Boolean> isCustomerReady(Customer customer) {
        String key = keyBuilder.queue(customer.getActionGroupId(), WaitStatus.READY);
        return stringRedisTemplate.opsForZSet().rank(key, customer.getCustomerId())
                .map(rank -> rank >= 0);
    }

    public Mono<String> getCustomerTokenById(String customerId) {
        var actionId = CustomerUtil.parseActionIdFromCustomerId(customerId);
        String key = keyBuilder.customerToken(actionId);
        return stringRedisTemplate.opsForHash().get(key, customerId)
                .map(v -> ((String) v));
    }

    public Mono<Boolean> putCustomerToken(String customerId, String token) {
        var actionId = CustomerUtil.parseActionIdFromCustomerId(customerId);
        String key = keyBuilder.customerToken(actionId);
        return stringRedisTemplate.opsForHash().put(key, customerId, token);
    }

    public Mono<Long> deleteCustomerTokenById(String customerId) {
        var actionId = CustomerUtil.parseActionIdFromCustomerId(customerId);
        String key = keyBuilder.customerToken(actionId);
        return stringRedisTemplate.opsForHash().remove(key, customerId);
    }
}