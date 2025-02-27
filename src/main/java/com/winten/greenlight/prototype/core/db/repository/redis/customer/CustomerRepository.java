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

    public CustomerRepository(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        return null;
    }
    public Mono<CustomerQueueInfo> getCustomerStatus(Customer customer) {
        String customerId = customer.getCustomerId();
        Mono<CustomerQueueInfo> waitingCheck = fetchFromQueue(WaitingPhase.WAITING, customerId);
        Mono<CustomerQueueInfo> readyCheck = fetchFromQueue(WaitingPhase.READY, customerId);

        return Mono.zip(waitingCheck.switchIfEmpty(Mono.just(new CustomerQueueInfo())),
                readyCheck.switchIfEmpty(Mono.just(new CustomerQueueInfo())))
            .map(tuple -> {
                CustomerQueueInfo waitingEntity = tuple.getT1();
                CustomerQueueInfo readyEntity = tuple.getT2();

                if (waitingEntity.getCustomerId() != null && waitingEntity.getWaitingPhase() != null) {
                    return waitingEntity;
                } else if (readyEntity.getCustomerId() != null && readyEntity.getWaitingPhase() != null) {
                    return readyEntity;
                } else {
                    return new CustomerQueueInfo();
                }
            });
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return Mono.just(CustomerZSetEntity.of(customer))
                .flatMap(entity -> redisTemplate.opsForZSet()
                        //삭제처리
                        .remove(customer.getWaitingPhase().queueName(), entity.getCustomerId())
                        //삭제된 데이터 없는 경우 Mono.empty() 반환
                        .flatMap(removedCount -> removedCount > 0 ? Mono.just(customer) : Mono.empty())
                );
    }

}

    private Mono<CustomerQueueInfo> fetchFromQueue(WaitingPhase phase, String customerId) {
        String queueName = phase.queueName();
        return Mono.zip(
            redisTemplate.opsForZSet().rank(queueName, customerId),  // 위치
            redisTemplate.opsForZSet().size(queueName)               // 큐 크기
        ).map(tuple -> {
            Long position = tuple.getT1();
            Long queueSize = tuple.getT2();
            return CustomerQueueInfo.builder()
                .customerId(position != null ? customerId : null)
                .position(position)
                .queueSize(queueSize)
                .waitingPhase(position != null ? phase : null)
                .estimatedWaitTime(null) // Service에서 계산
                .build();
        }).switchIfEmpty(Mono.just(new CustomerQueueInfo()));
    }
}
