package com.winten.greenlight.core.api.controller.queue;

import com.winten.greenlight.core.domain.action.ActionService;
import com.winten.greenlight.core.domain.customer.CustomerConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ActionController {
    private final ActionService actionService;
    private final CustomerConverter customerConverter;
    /**
     * Action 목록 조회기능 추가
     *
     * @param request
     * @param greenlightApiKey
     * @return
     */
    // TODO 보안키 적용 필요함
    @GetMapping("/api/v1/config")
    public Mono<ResponseEntity<QueueConfigResponse>> getGreenlightStatus(
            QueueConfigRequest request,
            @RequestHeader(value = "X-GREENLIGHT-API-KEY", required = false) String greenlightApiKey // TODO Site ID로 조회 가능한 Action 목록 제어
    ) {
        return actionService.getActionConfig(request.getVersion())
                .map(config -> ResponseEntity.ok(customerConverter.toResponse(config)));
    }
}