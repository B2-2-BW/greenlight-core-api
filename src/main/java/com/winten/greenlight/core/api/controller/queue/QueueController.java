package com.winten.greenlight.core.api.controller.queue;

import com.winten.greenlight.core.api.controller.customer.CustomerLeaveRequest;
import com.winten.greenlight.core.api.controller.customer.TicketVerificationResponse;
import com.winten.greenlight.core.api.controller.queue.dto.EntryRequest;
import com.winten.greenlight.core.domain.queue.QueueService;
import com.winten.greenlight.core.domain.customer.EntryTicket;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 외부 HTTP 요청을 받아 Application Service로 전달하고, 결과를 응답(Response)합니다.
 * */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class QueueController {
    private static final String GREENLIGHT_ID_HEADER = "X-GREENLIGHT-ID";
    private final QueueService queueService;

    /**
     * 대기열 상태 확인 API 엔드포인트입니다.
     * 사용자가 특정 액션(URL)에 접근할 때 호출되어, 대기열 적용 대상인지를 판단하여 상태를 반환합니다.
     *
     * @param request      대기열 진입 요청 정보 (actionId, requestParams)
     * @param greenlightId (Optional) 고객이 보유한 대기열 ID
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    @PostMapping("/api/v1/queue/check-or-enter")
    public Mono<EntryTicket> checkOrEnterQueue(
            @RequestBody EntryRequest request,
            @RequestHeader(name = GREENLIGHT_ID_HEADER, required = false) String greenlightId
    ) {
        if (request.getActionId() == null) {
            return Mono.error(new CoreException(ErrorType.BAD_REQUEST, "actionId is required."));
        }
        return queueService.checkOrEnterQueue(request.getActionId(), request.getDestinationUrl(), greenlightId);
    }

    // 기존 CustomerController에 있던 API를 QueueController로 이관. 레거시 지원을 위해 기존 매핑 유지
    @PostMapping(value={"/api/v1/customer/verify", "/api/v1/queue/verify"})
    public Mono<ResponseEntity<TicketVerificationResponse>> verifyTicket(
            @RequestHeader(name = GREENLIGHT_ID_HEADER) String greenlightId
    ) {
        return queueService.verifyTicket(greenlightId)
                    .map(ResponseEntity::ok);
    }

    @PostMapping("/api/v1/queue/leave")
    public Mono<ResponseEntity<Void>> deleteCustomer(
            @RequestBody CustomerLeaveRequest request
    ) {
        return queueService.deleteCustomerFromQueue(request.getGreenlightToken())
                .thenReturn(ResponseEntity.ok().build());
    }
}