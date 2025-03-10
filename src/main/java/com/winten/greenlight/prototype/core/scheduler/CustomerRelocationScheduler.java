package com.winten.greenlight.prototype.core.scheduler;

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
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private Event event;
    private String eventName = "test";


    public CustomerRelocationScheduler(CachedEventService cachedEventService, ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.cachedEventService = cachedEventService;
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
                                    .flatMapMany(n -> {
                                        if(n==0){ // 넘기는 고객의 수가 0일 경우, Flux.empty를 반환하고 종료한다.
                                            log.info("No customers to relocated. Skipping transaction");
                                            return Flux.empty();
                                        }
                                        //1) watingQueue에서 상위 N명의 고객을 가져온다
                                        return reactiveRedisTemplate.opsForZSet().rangeWithScores(WaitingPhase.WAITING.queueName(), Range.closed(0L, ((long)n-1L)))
                                                .collectList()
                                                .flatMapMany(customers -> {
                                                    //전체 customers의 List를 기준으로 반복문을 실행 한다.
                                                    return Flux.fromIterable(customers)
                                                            .flatMap(customer -> Mono.justOrEmpty(customer.getValue())
                                                                    .filter(value -> !value.isEmpty())
                                                                    .flatMap(value -> {
                                                                        double score = customer.getScore();
                                                                        return reactiveRedisTemplate.opsForZSet().add(WaitingPhase.READY.queueName(), value, score)//ready Queue insert
                                                                                .flatMap(success -> {
                                                                                    if(Boolean.TRUE.equals(success)) {
                                                                                        return reactiveRedisTemplate.opsForZSet().remove(WaitingPhase.WAITING.queueName(), value)
                                                                                                .doOnSuccess(result -> log.info("Successfully relocated customer {}: {}", value, result));
                                                                                    } else {
                                                                                        log.warn("Failed to add customer to READY queue: {}. Skipping transaction", value);
                                                                                        return Mono.empty();
                                                                                    }
                                                                                })
                                                                                .onErrorResume(error -> {
                                                                                    log.error("Error relocating customer {}: {}", value, error.getMessage());
                                                                                    return Mono.empty();
                                                                                });
                                                                    })
                                                            );
                                                });
                                    });
                })
                .doOnError(error -> log.error("Scheduler encountered an error: {}", error.getMessage(), error))
                .onErrorContinue((error, obj) -> log.warn("Ignoring error and continuing: {}", error.getMessage()))
                .subscribe();
    }
}
