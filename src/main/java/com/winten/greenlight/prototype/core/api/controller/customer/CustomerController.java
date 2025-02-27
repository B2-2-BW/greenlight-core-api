package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final CachedEventService cachedEventService;

    // TODO [사용자] 이벤트 대기열 참가신청 API https://github.com/B2-2-BW/greenlight-prototype-core-api/issues/2
    @PostMapping(value="")
    public Mono<ResponseEntity<CustomerRegistrationResponseDto>> createCustomer(@RequestBody final CustomerRequestDto requestDto) {
        long score = System.currentTimeMillis(); // score 채번은 선착순 순번을 최대한 보장하기 위해 최상단 고정

        // customerId는 eventName:tsid 형식으로 생성됨. 예시. event-live:ABC123DEF456
        // redis key는 customerId로 바로 조회 가능
        Customer customer = new Customer();
        customer.setScore(score);
        // cachedEventService에서 requestDto.eventId가 유효한지 검증
        // Customer 조회
        // CustomerStatus 조회
        // 성공시 HttpStatus 201 CREATED 반환
        return null;
    }

    // TODO [사용자] 이벤트 대기상태 조회 API https://github.com/B2-2-BW/greenlight-prototype-core-api/issues/3
    @GetMapping("{customerId}/status")
    public Mono<ResponseEntity<CustomerQueueInfoResponseDto>> getCustomerQueueInfo(@BindParam final CustomerRequestDto requestDto) {
        // customerId는 eventName:tsid 형식으로 생성됨. 예시. event-live:ABC123DEF456
        // redis key는 customerId로 바로 조회 가능
        // CustomerStatus 조회
        // Waiting 상태인지 조회
        // Ready 상태인지 조회
        // 없으면 에러
        // 성공시 HttpStatus 200 OK 반환

        Customer customer = new Customer();
        customer.setCustomerId(requestDto.getCustomerId());

        return customerService.getCustomerQueueInfo(customer)
            .map(info -> CustomerQueueInfoResponseDto.builder()
                .customerId(info.getCustomerId())
                .position(info.getPosition())
                .queueSize(info.getQueueSize())
                .estimatedWaitTime(info.getEstimatedWaitTime())
                .waitingPhase(info.getWaitingPhase())
                .build())
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    // TODO 현재 고객을 대기열에서 제거
    @DeleteMapping("{customerId}")
    public Mono<ResponseEntity<CustomerDeletionResponseDto>> deleteCustomer(@BindParam final CustomerRequestDto requestDto) {
        // customerId는 eventName:tsid 형식으로 생성됨. 예시. event-live:ABC123DEF456
        // redis key는 customerId로 바로 조회 가능
        // 삭제 실패 시 CoreException throw
        // 성공시 HttpStatus 200 OK 반환
        return null;
    }
}
