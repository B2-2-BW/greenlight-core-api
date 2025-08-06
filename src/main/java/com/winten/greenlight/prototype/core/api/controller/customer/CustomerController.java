package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    // TODO
    //  1. 서비스 입장
//    @PostMapping("{actionId}/check-in")
//    public Mono<ResponseEntity<CustomerEntryResponse>> requestEntry(
//            @RequestHeader("x-greenlight-ticket") String greenlightTicket,
//            @RequestBody CustomerEntryRequest request
//    ) {
//
//        long timestamp = System.currentTimeMillis();
//        return customerService.requestEntry(request.toCustomerEntry(timestamp))
//                .map(entry -> CustomerEntryResponse.of(entry))
//                .map(res -> ResponseEntity.ok(res));
//    }

    // TODO
    //  2. 입장권 검증요청
    @PostMapping("verify")
    public Mono<ResponseEntity<TicketVerificationResponse>> verifyTicket(
            @RequestHeader(name = "X-GREENLIGHT-TOKEN") String token
    ) {
        return customerService.verifyTicket(token)
                .map(ResponseEntity::ok);
    }

    @PostMapping("leave")
    public Mono<ResponseEntity<Void>> deleteCustomer(
            @RequestBody CustomerLeaveRequest request
    ) {
        return customerService.deleteCustomerFromQueue(request.getGreenlightToken())
                .thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping("/accesslog/dummy")
    public Mono<ResponseEntity<Void>> insertDummyAccessLog(@RequestParam Long actionGroupId) {
        return customerService.insertTestAccesslog(actionGroupId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}