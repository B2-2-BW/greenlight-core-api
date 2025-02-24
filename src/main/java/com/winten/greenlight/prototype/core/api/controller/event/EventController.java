package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/events")
public class EventController {
    private final CachedEventService cachedEventService;

    public EventController(CachedEventService cachedEventService) {
        this.cachedEventService = cachedEventService;
    }

    @GetMapping("{eventId}") // TODO [이벤트] 이벤트 정보 조회 GET API https://github.com/B2-2-BW/greenlight-prototype-core-api/issues/1
    public Mono<ResponseEntity<EventResponseDto>> getEvent(@BindParam final EventRequestDto requestDto) {
        return cachedEventService.getEvent(requestDto.toEvent())
                        .flatMap(event -> Mono.just(ResponseEntity.ok(new EventResponseDto(event))));
    }


    @DeleteMapping("{eventId}/cache")
    public Mono<ResponseEntity<EventCacheResponseDto>> invalidateEventCache(@BindParam final EventRequestDto requestDto) {
        return cachedEventService.invalidateEventCache(requestDto.toEvent())
                .flatMap(event -> Mono.just(ResponseEntity.ok(new EventCacheResponseDto(event.getEventId(), "캐시 초기화 완료."))));
    }
}