package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import com.winten.greenlight.prototype.core.domain.event.Event;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
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

    @PostMapping(value = "")
    public Mono<ResponseEntity<CustomerRegistrationResponseDto>> createCustomer(@RequestBody final CustomerRequestDto requestDto) {
        long score = System.currentTimeMillis(); // 선착순 보장을 위해 최상단에서 score 채번

        return Mono.defer(() -> {
                Customer customer = new Customer();
                customer.setScore(score);

                Event event = new Event();
                event.setEventName(requestDto.getEventName()); // 이벤트 객체 생성

                return customerService.createCustomer(customer, event)
                        .map(responseCustomer -> ResponseEntity
                                .status(201) // 201 CREATED 반환
                                .body(new CustomerRegistrationResponseDto(
                                        responseCustomer.getCustomerId(),
                                        responseCustomer.getScore(),
                                        new CustomerQueueInfo()
                                )));
            })
            .doOnSuccess(response -> log.info("Customer created successfully: {}", response))
            .doOnError(error -> log.error("Error while creating customer", error))
            .onErrorResume(error -> {
                if (error instanceof CoreException) {
                    return Mono.just(ResponseEntity
                            .badRequest()
                            .body(new CustomerRegistrationResponseDto(null, 0, null))
                    );
                }
                return Mono.just(ResponseEntity
                        .status(500)
                        .body(new CustomerRegistrationResponseDto(null, 0, null))
                );
            });
    }

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

    @DeleteMapping("{customerId}")
    public Mono<ResponseEntity<CustomerDeletionResponseDto>> deleteCustomer(@BindParam final CustomerRequestDto requestDto) {
        // customerId는 eventName:tsid 형식으로 생성됨. 예시. event-live:ABC123DEF456
        // redis key는 customerId로 바로 조회 가능
        // 삭제 실패 시 CoreException throw
        // 성공시 HttpStatus 200 OK 반환

        //처음에 조회해서 조회할 고객이 있어야만 delete하도록 하고싶음~
        return customerService.deleteCustomer(new Customer(requestDto.getCustomerId(), 0L, WaitingPhase.WAITING))
                .flatMap(customer -> Mono.just(ResponseEntity.ok(new CustomerDeletionResponseDto(customer.getCustomerId()))))
                .switchIfEmpty(Mono.error(new CoreException(ErrorType.DEFAULT_ERROR, "삭제할 대상 없음"))); //삭제 실패 시 CoreException throw
    }
}
