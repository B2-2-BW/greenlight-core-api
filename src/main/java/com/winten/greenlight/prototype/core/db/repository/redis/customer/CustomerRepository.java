package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class CustomerRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public CustomerRepository( ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        return null;
    }

    public Mono<CustomerZSetEntity> getCustomerStatus(Customer customer) {
        String customerId = customer.getCustomerId();
        Mono<CustomerZSetEntity> waitingCheck = fetchFromQueue(WaitingPhase.WAITING, customerId);
        Mono<CustomerZSetEntity> readyCheck = fetchFromQueue(WaitingPhase.READY, customerId);

        return Mono.zip(waitingCheck.switchIfEmpty(Mono.just(new CustomerZSetEntity())),
                readyCheck.switchIfEmpty(Mono.just(new CustomerZSetEntity())))
            .map(tuple -> {
                CustomerZSetEntity waitingEntity = tuple.getT1();
                CustomerZSetEntity readyEntity = tuple.getT2();

                if (waitingEntity.getCustomerId() != null && waitingEntity.getWaitingPhase() != null) {
                    return waitingEntity;
                } else if (readyEntity.getCustomerId() != null && readyEntity.getWaitingPhase() != null) {
                    return readyEntity;
                } else {
                    return new CustomerZSetEntity(); // null 상태 반환
                }
            });
    }

    private Mono<CustomerZSetEntity> fetchFromQueue(WaitingPhase phase, String customerId) {
        String queueName = phase.queueName();
        return Mono.zip(
            redisTemplate.opsForZSet().rank(queueName, customerId),  // 위치
            redisTemplate.opsForZSet().size(queueName)               // 큐 크기
        ).map(tuple -> {
            Long position = tuple.getT1();
            Long queueSize = tuple.getT2();
            CustomerZSetEntity entity = new CustomerZSetEntity();
            entity.setCustomerId(position != null ? customerId : null);
            entity.setWaitingPhase(position != null ? phase : null);
            entity.setScore(position != null ? position.doubleValue() : 0.0); // position을 score로 저장
            entity.setQueueSize(queueSize);
            return entity;
        }).switchIfEmpty(Mono.just(new CustomerZSetEntity()));
    }
}
