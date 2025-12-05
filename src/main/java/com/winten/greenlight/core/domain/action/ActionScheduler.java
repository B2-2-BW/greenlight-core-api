package com.winten.greenlight.core.domain.action;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionScheduler {
    private final ConfigVersionContextHolder configVersionContextHolder;
    private final CachedActionService cachedActionService;
    private final ActionService actionService;
    private Disposable subscription;

    @PostConstruct
    void init() {
        this.subscription = Flux.interval(Duration.ofSeconds(10))
                .flatMap(tick -> syncActionConfig()
                                    .doOnError(e -> log.error("[ActionScheduler] syncActionConfig error", e))
                                    .onErrorResume(e -> Mono.empty())
                )
                .subscribe();
    }

    private Mono<Void> syncActionConfig() {
        return actionService.getCurrentActionVersion()
                .flatMap(actualVersion -> {
                    if (!actualVersion.equals(configVersionContextHolder.get())) {
                        log.info("버전 변경 감지");
                        return cachedActionService.invalidateActionListCache()
                                .then(Mono.fromRunnable(() -> configVersionContextHolder.set(actualVersion)));
                    } else {
                        return Mono.empty();
                    }
                });
    }

    @PreDestroy
    public void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}