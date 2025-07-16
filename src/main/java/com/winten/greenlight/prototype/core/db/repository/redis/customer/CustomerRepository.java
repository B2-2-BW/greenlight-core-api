package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import com.winten.greenlight.prototype.core.support.util.RedisMemberBuilder;
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
    private final RedisMemberBuilder memberBuilder;

    public Mono<Customer> enqueueCustomer(Customer customer, Action action) {
        String waitKey = keyBuilder.queue(action.getActionGroupId(), WaitStatus.WAITING);
        return Mono.empty();
//        return redisTemplate.opsForZSet()
//                    .add(customer.getWaitStatus().queueName(), customer.getCustomerId(), customer.getScore())
//                    .<Customer>handle((success, sink) -> {
//                        if (Boolean.TRUE.equals(success)) {
//                            sink.next(customer);
//                        } else {
//                            sink.error(CoreException.of(ErrorType.REDIS_ERROR, "Customer Not Found"));
//                        }
//                    })
//                    .onErrorResume(e -> Mono.error(CoreException.of(ErrorType.REDIS_ERROR, e.getMessage())));
    }
    public Mono<CustomerQueueInfo> getCustomerStatus(Customer customer) {
        return Mono.empty();
//        String customerId = customer.getCustomerId();
//        WaitStatus waitStatus = customer.getWaitStatus();
//        return Mono.zip(
//                    redisTemplate.opsForZSet().rank(waitStatus.queueName(), customerId),  // 위치
//                    redisTemplate.opsForZSet().size(waitStatus.queueName())               // 큐 크기
//            ).map(tuple -> {
//                Long position = tuple.getT1();
//                Long queueSize = tuple.getT2();
//                return CustomerQueueInfo.builder()
//                        .customerId(customerId)
//                        .position(position)
//                        .queueSize(queueSize)
//                        .waitStatus(waitStatus)
//                        .estimatedWaitTime(null) // Service에서 계산
//                        .build();
//            });
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        var key = keyBuilder.queue(customer.getActionGroupId(), WaitStatus.READY);
        var member = memberBuilder.queue(customer.getActionId(), customer.getCustomerId());
        return redisTemplate.opsForZSet().remove(key, member)
                .flatMap(removedCount -> {
                    if (removedCount > 0) {
                        return Mono.just(customer);
                    } else {
                        return Mono.error(CoreException.of(ErrorType.INVALID_TOKEN, "삭제할 고객을 찾을 수 없습니다. " + customer));
                    }
                })
        ;
    }

    public Mono<Customer> getCustomerFromReadyQueue(Customer customer) {
        var key = keyBuilder.queue(customer.getActionGroupId(), WaitStatus.READY);
        var member = memberBuilder.queue(customer.getActionId(), customer.getCustomerId());
        return redisTemplate.opsForZSet().rank(key, member)
                .defaultIfEmpty(-1L) // 없을 경우 -1 리턴
                .map(rank -> rank >= 0 ? WaitStatus.READY : WaitStatus.UNKNOWN) // READY queue에 있는지 여부를 WaitStatus로 변환
                .map(waitStatus -> {
                    customer.setWaitStatus(waitStatus);
                    return customer;
                });
    }

    public Mono<Customer> enqueueCustomerToEntered(Customer customer) {
        var key = keyBuilder.queue(customer.getActionGroupId(), WaitStatus.ENTERED);
        var member = memberBuilder.queue(customer.getActionId(), customer.getCustomerId());
        return redisTemplate.opsForZSet().add(key, member, System.currentTimeMillis())
                .flatMap(result -> {
                    if (result) {
                        customer.setWaitStatus(WaitStatus.ENTERED);
                        return Mono.just(customer);
                    } else {
                        return Mono.error(CoreException.of(ErrorType.REDIS_ERROR, "Failed to add customer to queue. Customer: " + customer));
                    }
                });
    }
}