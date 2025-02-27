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


    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        return customerRepository.getCustomerStatus(customer)
            .map(info -> {
                if (info.getPosition() != null && info.getQueueSize() != null) {
                    info.setEstimatedWaitTime(info.getPosition() * 60); // 예시 계산
                }
                return info;
            })
            .filter(info -> info.getCustomerId() != null && info.getWaitingPhase() != null);
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
    }

}
