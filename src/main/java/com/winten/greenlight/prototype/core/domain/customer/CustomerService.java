package com.winten.greenlight.prototype.core.domain.customer;

import com.github.f4b6a3.tsid.TsidCreator;
import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
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
    private final CachedEventService cachedEventService;

    public Mono<Customer> createCustomer(Customer customer, Event event) {
        return Mono.defer(() -> {
            String customerId = event.getEventName() + ":" + TsidCreator.getTsid();
            return customerRepository.createCustomer(new Customer(customerId, customer.getScore(), WaitingPhase.WAITING));
        })
        .onErrorResume(error -> Mono.error(CoreException.of(ErrorType.DEFAULT_ERROR, error.getMessage())));
    }


    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        return customerRepository.getCustomerStatus(Customer.waiting(customer.getCustomerId())) // waiting 조회
                .zipWith(cachedEventService.getEventByName(customer.toEvent()))
                .flatMap(tuple -> {
                    CustomerQueueInfo queueInfo = tuple.getT1();
                    Event event = tuple.getT2();
                    queueInfo.setEstimatedWaitTime(event.getQueueBackpressure() > 0 ? queueInfo.getPosition()/event.getQueueBackpressure() : -1L);
                    return Mono.just(queueInfo);
                })
                .switchIfEmpty(customerRepository.getCustomerStatus(Customer.ready(customer.getCustomerId()))) // 없다면 ready 조회
                .switchIfEmpty(Mono.error(new CoreException(ErrorType.CUSTOMER_NOT_FOUND))); // waiting과 ready에 둘 다 없다면 오류
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return customerRepository.deleteCustomer(customer);
    }

    public Flux<Customer> relocateCustomer(long eventBackPressure) {
        if (eventBackPressure < 1) { // 넘기는 고객의 수가 0일 경우, Flux.empty를 반환하고 종료한다.
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