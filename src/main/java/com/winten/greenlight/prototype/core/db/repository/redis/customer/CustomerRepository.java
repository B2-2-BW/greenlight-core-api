package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder keyBuilder;

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
        return Mono.empty();
//        return Mono.just(CustomerEntity.of(customer))
//                .flatMap(entity -> redisTemplate.opsForZSet()
//                        //삭제처리
//                        .remove(customer.getWaitStatus().queueName(), entity.getCustomerId())
//                        //삭제된 데이터 없는 경우 Mono.empty() 반환
//                        .flatMap(removedCount -> removedCount > 0 ? Mono.just(customer) : Mono.empty())
//                );
    }
}