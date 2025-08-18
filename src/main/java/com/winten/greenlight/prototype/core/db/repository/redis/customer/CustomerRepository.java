package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;

    public Mono<Customer> enqueueCustomer(Customer customer, WaitStatus waitStatus) {
        String key = keyBuilder.queue(customer.getActionGroupId(), waitStatus);
        return redisTemplate.opsForZSet()
                .add(key, customer.getCustomerId(), customer.getScore())
                .flatMap(result -> result
                        ? Mono.just(customer)
                        : Mono.error(CoreException.of(ErrorType.REDIS_ERROR, "Redis insert failed. customer=" + customer + ", waitStatus=" + waitStatus))
                );
    }

    /**
     * 대기열에서 고객 삭제처리
     * @param customer actionGroupId 및 customerId 필수
     * @param waitStatus
     * @return
     */
    public Mono<Long> deleteCustomer(Customer customer, WaitStatus waitStatus) {
        String key = keyBuilder.queue(customer.getActionGroupId(), waitStatus);
        return redisTemplate.opsForZSet().remove(key, customer.getCustomerId());
    }

    public Mono<Boolean> isCustomerReady(Customer customer) {
        String key = keyBuilder.queue(customer.getActionGroupId(), WaitStatus.READY);
        return redisTemplate.opsForZSet().rank(key, customer.getCustomerId())
                .map(rank -> rank >= 0);
    }

}