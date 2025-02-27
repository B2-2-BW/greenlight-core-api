package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.github.f4b6a3.tsid.TsidCreator;
import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
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
                            sink.error(CoreException.of(ErrorType.REDIS_ERROR, "Customer Not Found"));
                        }
                    })
                    .onErrorResume(e -> Mono.error(CoreException.of(ErrorType.REDIS_ERROR, e.getMessage())));
        });
    }

    public Mono<CustomerQueueInfo> getCustomerStatus(Customer customer) {
        return null;
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }
}