package com.winten.greenlight.core.domain.customer;

import com.winten.greenlight.core.api.controller.customer.TicketVerificationResponse;
import com.winten.greenlight.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.core.support.util.JwtUtil;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final ActionEventPublisher actionEventPublisher;
    private final ActionRepository actionRepository;


    // TODO
    //  2. 입장권 검증요청
    //    - 입장권이 유효하지 않은 경우 대기열로 redirect
    //    - 유효한경우 진입 허용
    //    - 입장권 사용처리


    public Mono<Boolean> insertTestRequestLog(Long actionGroupId) {
        String customerId = TSID.fast().toString();
        return actionRepository.putRequestLog(actionGroupId, customerId);
    }
}