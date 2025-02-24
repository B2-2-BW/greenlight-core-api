package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // TODO [사용자] 이벤트 대기열 참가신청 API https://github.com/B2-2-BW/greenlight-prototype-core-api/issues/2
    @PostMapping("/{customerId}")
    public CustomerResponseDto createQueueMember(@BindParam final String customerId, @RequestBody final CustomerRequestDto requestDto) {
        return null;
    }

    // TODO [사용자] 이벤트 대기상태 조회 API https://github.com/B2-2-BW/greenlight-prototype-core-api/issues/3
    @GetMapping("{customerId}/status")
    public CustomerResponseDto getQueueMemberStatus(@BindParam final String customerId) {
        return null;
    }

    // TODO 현재 고객을 대기열에서 제거
    @DeleteMapping("{customerId}")
    public void deleteQueueMember(@BindParam final String customerId) {
    }
}