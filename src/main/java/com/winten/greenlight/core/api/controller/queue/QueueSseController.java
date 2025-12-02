package com.winten.greenlight.core.api.controller.queue;

import com.winten.greenlight.core.domain.action.CachedActionService;
import com.winten.greenlight.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.core.domain.queue.QueueSseService;
import com.winten.greenlight.core.support.util.CustomerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/waiting")
@RequiredArgsConstructor
public class QueueSseController {
    private final QueueSseService queueSseService;
    private final CachedActionService cachedActionService;

    // SSE 연동
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CustomerQueueInfo>> connectSse(
            @RequestParam String customerId
    ) {

        if (customerId == null || customerId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId가 존재하지 않습니다.");
        }
        var actionId = CustomerUtil.parseActionIdFromCustomerId(customerId);
        return cachedActionService.getActionById(actionId)   // Mono<Action>
                .flatMapMany(action -> queueSseService.connect(action.getActionGroupId(), customerId)) // Flux<CustomerQueueInfo
                .map(queueInfo -> ServerSentEvent.builder(queueInfo).build())
                .onErrorResume(e -> { // unexpected
                    log.warn("Unexpected error", e);
                    return Flux.empty(); // close stream
                });
    }

    // 고객 상태 조회
    // 고객이 어떤 queue에 들어가있는지 확인
//    @GetMapping("/status")
    public Mono<CustomerQueueInfo> findUserQueueStatus(Long actionGroupId, String customerId) {
        return queueSseService.findUserQueueInfo(actionGroupId, customerId);
    }
}