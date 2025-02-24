package com.winten.greenlight.prototype.core.domain.event;

import com.winten.greenlight.prototype.core.db.repository.event.EventRepository;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CachedEventService {
    private final EventRepository eventRepository;

    public CachedEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Cacheable(cacheNames = "event", key = "#event.eventId")
    public Mono<Event> getEvent(Event event) {
        return eventRepository.getEvent(event)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.EVENT_NOT_FOUND, "이벤트를 찾을 수 없습니다. eventId: " + event.getEventId())));
    }

    @CachePut(cacheNames = "event", key = "#event.eventId")
    public Mono<Event> invalidateEventCache(Event event) {
        return eventRepository.getEvent(event)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.EVENT_NOT_FOUND, "이벤트를 찾을 수 없습니다. eventId: " + event.getEventId())));
    }
}