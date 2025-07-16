package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerEntry;
import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final JwtUtil jwtUtil;

    // TODO
    //  1. 서비스 입장
    @PostMapping("{actionId}/check-in")
    public Mono<ResponseEntity<CustomerEntryResponse>> requestEntry(
            @RequestBody CustomerEntryRequest request
    ) {
        long timestamp = System.currentTimeMillis();
        return customerService.requestEntry(request.toCustomerEntry(timestamp))
                .map(entry -> CustomerEntryResponse.of(entry))
                .map(res -> ResponseEntity.ok(res));
    }

    // TODO
    //  2. 입장권 검증요청
    @PostMapping("verify")
    public Mono<ResponseEntity<TicketVerificationResponse>> verifyTicket(
            @RequestHeader("X-GREENLIGHT-TOKEN") String greenlightToken
    ) {
        return customerService.verifyTicket(greenlightToken)
                .map(TicketVerificationResponse::of)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> Mono.just(ResponseEntity.ok(TicketVerificationResponse.of(error))))
        ;
    }

    @GetMapping("random")
    public Mono<String> random() {
        return Mono.just(
                jwtUtil.generateToken(
                        CustomerEntry.builder()
                            .customerId(TSID.fast().toString())
                            .actionId(1L)
                            .build()
                )
        );
    }
}