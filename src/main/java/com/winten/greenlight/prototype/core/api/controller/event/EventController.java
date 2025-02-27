package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import com.winten.greenlight.prototype.core.domain.event.Event;
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

    @GetMapping("/{eventName}")
    public Mono<ResponseEntity<EventResponseDto>> getEvent(@BindParam final EventRequestDto requestDto) {
        return cachedEventService.getEventByName(requestDto.toEvent())
                        .flatMap(event -> Mono.just(ResponseEntity.ok(new EventResponseDto(event))));
    }


    @DeleteMapping("/{eventName}/cache")
    public Mono<ResponseEntity<EventResponseDto>> invalidateEventCache(@BindParam final EventRequestDto requestDto) {
        return cachedEventService.invalidateEventCache(requestDto.toEvent())
                .flatMap(event -> Mono.just(ResponseEntity.ok(new EventResponseDto(event))));
    }
}