package com.winten.greenlight.prototype.core.scheduler;

import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import com.winten.greenlight.prototype.core.domain.event.CachedEventService;
import com.winten.greenlight.prototype.core.domain.event.Event;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.time.Duration;

@Slf4j
@Component
public class CustomerRelocationScheduler {
    private final CachedEventService cachedEventService;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final Event event;
    private final ObservationRegistry observationRegistry;

    public CustomerRelocationScheduler(CachedEventService cachedEventService, ReactiveRedisTemplate<String, String> reactiveRedisTemplate, ObservationRegistry observationRegistry) {
        this.cachedEventService = cachedEventService;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.observationRegistry = observationRegistry;
        Event event = new Event();
        event.setEventName("test");
        this.event = event;
    }

    private Flux<Long> relocateCustomer() {
        Observation observation = Observation.start("schedule-customer-relocation", observationRegistry);
        observation.lowCardinalityKeyValue("class", "CustomerRelocationScheduler");
        observation.lowCardinalityKeyValue("method", "relocateCustomer");
        try (Observation.Scope scope = observation.openScope()) {
            //0) N을 가져온다 : CachedEventService를 이용
            return cachedEventService.getEventByName(event)
                    .map(Event::getQueueBackpressure)
                    .flatMapMany(n -> {
                        //1) watingQueue에서 상위 N명의 고객을 가져온다
                        return reactiveRedisTemplate.opsForZSet().rangeWithScores(WaitingPhase.WAITING.queueName(), Range.closed(0L, ((long) n - 1L)))
                                .contextWrite(Context.of(ObservationThreadLocalAccessor.KEY, observation))
                                .collectList()
                                .flatMapMany(customers -> {
                                    //전체 customers의 List를 기준으로 반복문을 실행 한다.
                                    return Flux.fromIterable(customers)
                                            .flatMap(customer -> {
                                                String value = customer.getValue();
                                                double score = customer.getScore();
                                                assert value != null;
                                                return reactiveRedisTemplate.opsForZSet().add(WaitingPhase.READY.queueName(), value, score)//ready Queue insert
                                                        .contextWrite(Context.of(ObservationThreadLocalAccessor.KEY, observation))
                                                        .then(reactiveRedisTemplate.opsForZSet().remove(WaitingPhase.WAITING.queueName(), value)
                                                                .contextWrite(Context.of(ObservationThreadLocalAccessor.KEY, observation))
                                                        )
                                                        .doOnNext(v -> log.info("Customer relocation completed: {}", value))
                                                        .doOnError(e -> log.error("Customer relocation completed with an error", e));
                                            });
                                });
                    })
                    .doOnError(observation::error)
                    .doOnTerminate(observation::stop);
        }
    }

    @PostConstruct
    public void scheduleCustomerRelocation() {
        Flux.interval(Duration.ofSeconds(5L), Schedulers.boundedElastic())
                .flatMap(tick -> relocateCustomer())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No customer was relocated");
                    return Mono.empty();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        v -> log.info("Customer relocation completed: {}", v),
                        error -> log.error("Customer relocation completed with an error", error),
                        () -> log.info("Customer relocation completed")
                );
    }
}