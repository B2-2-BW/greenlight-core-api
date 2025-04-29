package com.winten.greenlight.prototype.core.domain.sse;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final CustomerService customerService;

    public Mono<CustomerQueueInfo> getCustomerQueueInfo(String customerId) {

        //고객 객체 생성
        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        return customerService.getCustomerQueueInfo(customer);
    }
}
