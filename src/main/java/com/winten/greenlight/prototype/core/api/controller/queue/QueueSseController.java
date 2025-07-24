package com.winten.greenlight.prototype.core.api.controller.queue;

import com.winten.greenlight.prototype.core.domain.queue.QueueSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/waiting")
@RequiredArgsConstructor
public class QueueSseController {
    private final QueueSseService queueSseService;

    // TODO SSE 연동
    @GetMapping(value = "sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<?> connectSse(
            @RequestParam String entryId
    ) {
        //return Flux.interval(Duration.ofSeconds(10)) // 매10초마다
        //        .flatMap(tick -> sseService.getCustomerQueueInfo(requestDto.getCustomerId())) // 매 tick마다 새 데이터 조회
        //        .map(data -> ServerSentEvent.builder(data).build());

        return queueSseService.connect(entryId);
    }
}