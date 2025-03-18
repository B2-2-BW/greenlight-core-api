package com.winten.greenlight.prototype.core.domain.event;

import com.winten.greenlight.prototype.core.db.repository.redis.event.EventRepository;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CachedEventService {
    private final EventRepository eventCacheStore;

    public CachedEventService(EventRepository eventCacheStore) {
        this.eventCacheStore = eventCacheStore;
    }

    @Cacheable(cacheNames = "event", key = "#event.eventName")
    public Mono<Event> getEventByName(Event event) {
        return eventCacheStore.getEventByName(event)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.EVENT_NOT_FOUND, "이벤트를 찾을 수 없습니다. eventName: " + event.getEventName())));
    }

    @CacheEvict(cacheNames = "event", key = "#event.eventName")
    public Mono<Void> invalidateEventCache(Event event) {
        return Mono.empty();
    }
}