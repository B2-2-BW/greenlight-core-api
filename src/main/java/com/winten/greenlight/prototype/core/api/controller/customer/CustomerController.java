package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/action")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;


    @PostMapping("{actionId}/check-in")
    public Mono<ResponseEntity<CustomerEntryResponse>> requestEntry(
            @RequestBody CustomerEntryRequest request
    ) {
        long timestamp = System.currentTimeMillis();
        return customerService.requestEntry(request.toCustomerEntry(timestamp))
                .map(entry -> CustomerEntryResponse.of(entry))
                .map(res -> ResponseEntity.ok(res));
    }

    @PostMapping("verify")
    public Mono<ResponseEntity<CustomerEntryResponse>> verifyEntry(
            @RequestHeader String autho
    ) {
        return Mono.empty();
    }
}