package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.api.controller.customer.TicketVerificationResponse;
import com.winten.greenlight.prototype.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.prototype.core.domain.action.CachedActionService;
import com.winten.greenlight.prototype.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.publisher.ActionEvent;
import com.winten.greenlight.prototype.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CachedActionService cachedActionService;
    private final ObservationRegistry observationRegistry;
    private final JwtUtil jwtUtil;
    private final ActionEventPublisher actionEventPublisher;


    // TODO
    //  2. 입장권 검증요청
    //    - 입장권이 유효하지 않은 경우 대기열로 redirect
    //    - 유효한경우 진입 허용
    //    - 입장권 사용처리
    public Mono<TicketVerificationResponse> verifyTicket(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            return Mono.error(CoreException.of(ErrorType.INVALID_TOKEN, "유효하지 않은 입장권입니다."));
        }
        var customer = jwtUtil.getCustomerFromToken(token);
        customer.setScore(System.currentTimeMillis());
        return customerRepository.isCustomerReady(customer)
                .flatMap(isReady -> {
                    if (!isReady) {
                        return Mono.empty(); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                    }
                    return customerRepository.enqueueCustomer(customer, WaitStatus.ENTERED)
                            .then(customerRepository.deleteCustomer(customer, WaitStatus.READY))
                            .then(Mono.defer(() -> {
                                customer.setWaitStatus(WaitStatus.ENTERED);
                                return actionEventPublisher.publish(customer);
                            }))
                            .then(Mono.just(TicketVerificationResponse.success(customer)));
                })
                .switchIfEmpty(Mono.just(TicketVerificationResponse.fail(customer, "유효한 고객 ID를 찾을 수 없습니다.")))
                .onErrorResume(e -> Mono.error(CoreException.of(ErrorType.INTERNAL_SERVER_ERROR, "입장에 실패하였습니다 " + e.getMessage())))
                ;
    }
}