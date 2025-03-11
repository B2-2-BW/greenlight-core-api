package com.winten.greenlight.prototype.core.domain.customer;

import com.github.f4b6a3.tsid.TsidCreator;
import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.domain.event.Event;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public Mono<Customer> createCustomer(Customer customer, Event event) {
        return Mono.defer(() -> {
            String customerId = event.getEventName() + ":" + TsidCreator.getTsid();
            customer.setCustomerId(customerId); // 이벤트 이름으로 Customer ID 설정
            customer.setWaitingPhase(WaitingPhase.WAITING);
            return customerRepository.createCustomer(customer);
        });
//                .doOnSuccess(result -> log.info("Customer created: {}", result))
//                .doOnError(error -> log.error("Error creating customer", error));
    }


    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        return customerRepository.getCustomerStatus(customer)
            .map(info -> {
                if (info.getPosition() != null && info.getQueueSize() != null) {
                    info.setEstimatedWaitTime(info.getPosition()); // 예시 계산
                }
                return info;
            })
            .filter(info -> info.getCustomerId() != null && info.getWaitingPhase() != null);
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return customerRepository.deleteCustomer(customer);
    }

    public Flux<Customer> relocateCustomer(long eventBackPressure) {
        if(eventBackPressure<1){ // 넘기는 고객의 수가 0일 경우, Flux.empty를 반환하고 종료한다.
            log.info("No customers to relocated. Skipping transaction");
            return Flux.empty();
        }
        //1) watingQueue에서 상위 N명의 고객을 가져온 뒤, 순차적으로 add, remove 를 실행한다
        return customerRepository.getTopNCustomers(eventBackPressure)
                .flatMap(customerRepository::createCustomer)//ready Queue insert
                .doOnError(error -> log.error("Error while relocating add customer", error))
                .flatMap(customer -> {
                    customer.setWaitingPhase(WaitingPhase.WAITING);
                    return customerRepository.deleteCustomer(customer)
                            .doOnError(error -> log.error("Error while relocating remove customer", error));
                })
                .doOnNext(result -> log.info("Success to relocated: {}",result));
    }
}