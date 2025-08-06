package com.winten.greenlight.prototype.core.api.controller.queue;

import com.winten.greenlight.prototype.core.api.controller.queue.dto.EntryRequest;
import com.winten.greenlight.prototype.core.domain.queue.QueueApplicationService;
import com.winten.greenlight.prototype.core.domain.customer.EntryTicket;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 외부 HTTP 요청을 받아 Application Service로 전달하고, 결과를 응답(Response)합니다.
 * */
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueApplicationService queueApplicationService;

    /**
     * 대기열 상태 확인 API 엔드포인트입니다.
     * 사용자가 특정 액션(URL)에 접근할 때 호출되어, 대기열 적용 대상인지를 판단하여 상태를 반환합니다.
     *
     * @param request      대기열 진입 요청 정보 (actionId, requestParams)
     * @param greenlightToken (Optional) 고객이 보유한 대기열 토큰
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    @PostMapping("/check-or-enter")
    public Mono<EntryTicket> checkOrEnterQueue(
            @RequestBody EntryRequest request,
            @RequestParam(required = false) Map<String, String> requestParams,
            @RequestHeader(name = "X-GREENLIGHT-TOKEN", required = false) String greenlightToken
    ) {
        if (request.getActionId() == null) {
            return Mono.error(new CoreException(ErrorType.BAD_REQUEST, "actionId is required."));
        }
        long entryTimestamp = System.currentTimeMillis();
        return queueApplicationService.checkOrEnterQueue(request.getActionId(), request.getDestinationUrl(), greenlightToken, requestParams, entryTimestamp);
    }
}