package com.winten.greenlight.prototype.core.db.repository.redis.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
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

    public Mono<CustomerQueueInfo> getCustomerStatus(Customer customer) {
        return null;
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }
}