package com.winten.greenlight.prototype.core.api.controller.action;

import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        return customerService.checkIn(request.toCustomerEntry(timestamp))
                .map(entry -> CustomerEntryResponse.of(entry))
                .map(res -> ResponseEntity.ok(res));
    }

    @PostMapping("")
    public Mono<ResponseEntity<CustomerEntryResponse>> requestEntry() {
        return Mono.empty();
    }
}