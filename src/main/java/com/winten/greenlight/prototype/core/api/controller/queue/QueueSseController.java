package com.winten.greenlight.prototype.core.api.controller.queue;

import com.winten.greenlight.prototype.core.domain.action.CachedActionService;
import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.queue.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.queue.QueueSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/waiting")
@RequiredArgsConstructor
public class QueueSseController {
    private final QueueSseService queueSseService;
    private final CachedActionService cachedActionService;

    // SSE 연동
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CustomerQueueInfo>> connectSse(
            @RequestParam Long actionId,
            @RequestParam String customerId
    ) {

        if (actionId == null || customerId == null || customerId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actionId나 customerId가 존재하지 않습니다.");
        }

        return cachedActionService.getActionById(actionId)   // Mono<Action>
                .flatMapMany(action -> queueSseService.connect(action.getActionGroupId(), customerId)) // Flux<CustomerQueueInfo
                .map(queueInfo -> ServerSentEvent.builder(queueInfo).build());
    }


    // 고객 상태 조회
    // 고객이 어떤 queue에 들어가있는지 확인
    @GetMapping("/status")
    public Mono<CustomerQueueInfo> findUserQueueStatus(Long actionGroupId, String customerId) {
        return queueSseService.findUserQueueInfo(actionGroupId, customerId);
    }
}