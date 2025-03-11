package com.winten.greenlight.prototype.core.scheduler;

import com.winten.greenlight.prototype.core.domain.customer.CustomerService;
import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import com.winten.greenlight.prototype.core.domain.event.Event;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@Component
public class CustomerRelocationScheduler {
    private final CachedEventService cachedEventService;
    private final CustomerService customerService;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private Event event;
    private String eventName = "test";


    public CustomerRelocationScheduler(CachedEventService cachedEventService, CustomerService customerService, ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.cachedEventService = cachedEventService;
        this.customerService = customerService;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.event = new Event();
        event.setEventName(eventName);
    }

    @PostConstruct
    public void scheduleCustomerRelocation() {

        Flux.interval(Duration.ofSeconds(5L), Schedulers.boundedElastic())
                .flatMap(tick -> {
                    System.out.println("###############################호롤롤로#################" + tick);
                    //0) N을 가져온다 : CachedEventService를 이용
                    return cachedEventService.getEventByName(event)
                                    .map(Event::getQueueBackpressure)
                                    .flatMapMany(customerService::relocateCustomer);
                })
                .doOnError(error -> log.error("Scheduler encountered an error: {}", error.getMessage(), error))
                .onErrorContinue((error, obj) -> log.warn("Ignoring error and continuing: {}", error.getMessage()))
                .subscribe();
    }
}