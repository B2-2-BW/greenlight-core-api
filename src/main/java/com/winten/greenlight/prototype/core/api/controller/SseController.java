package com.winten.greenlight.prototype.core.api.controller;

import com.winten.greenlight.prototype.core.api.controller.customer.CustomerRequestDto;
import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import com.winten.greenlight.prototype.core.domain.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {
    private final SseService sseService;

    @GetMapping(value = "{customerId}",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CustomerQueueInfo>> connect(@BindParam final CustomerRequestDto requestDto) {
        return Flux.interval(Duration.ofSeconds(10)) // 매10초마다
                .flatMap(tick -> sseService.getCustomerQueueInfo(requestDto.getCustomerId())) // 매 tick마다 새 데이터 조회
                .map(data -> ServerSentEvent.builder(data).build());
    }
}