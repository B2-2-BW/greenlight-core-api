package com.winten.greenlight.prototype.core.api.controller.queue;

import com.winten.greenlight.prototype.core.api.controller.queue.dto.CheckOrEnterResponse;
import com.winten.greenlight.prototype.core.domain.queue.QueueApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
     * 대기열 진입 및 상태 확인 API 엔드포인트입니다.
     * 사용자가 특정 액션(URL)에 접근할 때 호출되어, 대기열 진입 여부를 판단하고 토큰을 발급합니다.
     *
     * @param actionUrl  사용자가 접근하려는 액션의 URL (예: "/products/limited-edition")
     * @param customerId 고객을 식별하는 고유 ID
     * @return Mono<CheckOrEnterResponse> 대기 상태 및 발급된 토큰 정보를 포함하는 응답
     */
    @GetMapping("/check-or-enter")
    public Mono<CheckOrEnterResponse> checkOrEnterQueue(ServerHttpRequest request) {
        // ServerHttpRequest에서 모든 쿼리 파라미터를 추출합니다.
        Map<String, String> requestParams = request.getQueryParams().toSingleValueMap();

        // 필수 파라미터인 actionUrl과 customerId를 추출합니다.
        String actionUrl = requestParams.get("actionUrl");
        String customerId = requestParams.get("customerId");

        // TODO: actionUrl 또는 customerId가 null일 경우에 대한 예외 처리 필요
        if (actionUrl == null || customerId == null) {
            return Mono.just(new CheckOrEnterResponse("BAD_REQUEST", null, null));
        }

        return queueApplicationService.checkOrEnterQueue(actionUrl, customerId, requestParams);
    }
}
