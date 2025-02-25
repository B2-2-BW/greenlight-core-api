package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public Mono<Customer> createCustomer(Customer customer, Event event) {
        return null;
    }

    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        return null;
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }

}