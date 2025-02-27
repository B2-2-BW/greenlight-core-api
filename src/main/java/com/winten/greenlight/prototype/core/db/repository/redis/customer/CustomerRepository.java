package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.github.f4b6a3.tsid.TsidCreator;
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
        return Mono.defer(() -> {
            String customerId = customer.getCustomerId() + ":" + TsidCreator.getTsid();
            Customer newCustomer = new Customer(customerId, customer.getScore(), WaitingPhase.WAITING);

            return redisTemplate.opsForZSet()
                    .add(newCustomer.getWaitingPhase().queueName(), newCustomer.getCustomerId(), newCustomer.getScore())
                    .<Customer>handle((success, sink) -> {
                        if (Boolean.TRUE.equals(success)) {
//                            log.info("Successfully saved ticket: {}", newCustomer);
                            sink.next(newCustomer);
                        } else {
                            sink.error(new RuntimeException("Failed to save customer in Redis."));
                        }
                    })
                    .onErrorResume(e -> {
//                        log.error("Failed to save ticket", e);
                        return Mono.error(e);
                    });
        });
    }

    public Mono<CustomerQueueInfo> getCustomerStatus(Customer customer) {
        return null;
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }
}