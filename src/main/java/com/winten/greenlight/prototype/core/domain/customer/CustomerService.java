package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerZSetEntity;
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


    public Mono<CustomerZSetEntity> getCustomerQueueInfo(Customer customer) {
        return customerRepository.getCustomerStatus(customer)
            .flatMap(entity -> {
                if (entity.getCustomerId() == null || entity.getWaitingPhase() == null) {
                    return Mono.empty(); // WAITING 또는 READY 상태가 아닌 경우
                }
                return Mono.just(entity);
            });
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }

}
