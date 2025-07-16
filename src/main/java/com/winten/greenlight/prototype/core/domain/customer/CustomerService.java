package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.domain.action.CachedActionService;
import com.winten.greenlight.prototype.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CachedActionService cachedActionService;
    private final ObservationRegistry observationRegistry;
    private final JwtUtil jwtUtil;

    // TODO
    //  1. 서비스 입장
    //    - 현재 액션의 액션그룹 조회
    //    - 액션그룹으로 대기필요여부 조회 (최대 활성사용자 수 - 현재 활성사용자 수 > 0)
    //    - 대기 필요한 경우 대기번호와 READY 상태의 입장권 발급하여 대기 없이 입장
    //    - 대기 필요 시 대기번호 부여하고 대기열로 redirect
    public Mono<CustomerEntry> requestEntry(CustomerEntry customerEntry) {
        return Mono.empty();
    }

    /**
     * CustomerEntry에서 token String을 생성하는 함수
     * @param customerEntry
     * @return JWT String
     */
    private Mono<String> createEntryTicketToken(CustomerEntry customerEntry) {
        return Mono.fromCallable(() -> jwtUtil.generateToken(customerEntry))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // TODO
    //  2. 입장권 검증요청
    //    - 입장권이 유효하지 않은 경우 대기열로 redirect
    //    - 유효한경우 진입 허용
    //    - 입장권 사용처리
    public Mono<Customer> verifyTicket(String token) {
        var ticket = jwtUtil.getEntryTicketFromToken(token);
        return cachedActionService.getActionById(ticket.getActionId())
                .flatMap(action -> {
                    // ready에 있는지 검사, 있다면 Entered로 이동시키고 허용하는 응답 반환
                    var customer = Customer.builder()
                            .customerId(ticket.getCustomerId())
                            .actionGroupId(action.getActionGroupId())
                            .actionId(action.getId())
                            .build();
                    return customerRepository.getCustomerFromReadyQueue(customer);
                })
                .flatMap(customer -> {
                    if (customer.getWaitStatus() == WaitStatus.READY) {
                        return customerRepository.deleteCustomer(customer)
                                .flatMap(deleted -> customerRepository.enqueueCustomerToEntered(customer));
                    } else {
                        return Mono.error(CoreException.of(ErrorType.INVALID_TOKEN, "유효하지 않은 입장권입니다."));
                    }
                })
        ;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        return null;
//        return Mono.defer(() -> {
//            String customerId = event.getEventName() + ":" + TSID.fast().toString();
//            return customerRepository.createCustomer(new Customer(customerId, customer.getScore(), WaitingPhase.WAITING));
//        })
//        .onErrorResume(error -> Mono.error(CoreException.of(ErrorType.DEFAULT_ERROR, error.getMessage())));
    }


    public Mono<CustomerQueueInfo> getCustomerQueueInfo(Customer customer) {
        return null;
//        return customerRepository.getCustomerStatus(Customer.waiting(customer.getCustomerId())) // waiting 조회
//                .zipWith(cachedEventService.getEventByName(customer.toEvent()))
//                .flatMap(tuple -> {
//                    CustomerQueueInfo queueInfo = tuple.getT1();
//                    Event event = tuple.getT2();
//                    queueInfo.setEstimatedWaitTime(event.getQueueBackpressure() > 0 ? queueInfo.getPosition()/event.getQueueBackpressure() : -1L);
//                    return Mono.just(queueInfo);
//                })
//                .switchIfEmpty(customerRepository.getCustomerStatus(Customer.ready(customer.getCustomerId()))) // 없다면 ready 조회
//                .switchIfEmpty(Mono.error(new CoreException(ErrorType.CUSTOMER_NOT_FOUND))); // waiting과 ready에 둘 다 없다면 오류
    }

    public Mono<Customer> deleteCustomer(Customer customer) {
        return null;
//        return customerRepository.deleteCustomer(customer);
    }
}