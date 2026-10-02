package com.winten.greenlight.core.support.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFailOpenProbe {
    private final LettuceConnectionFactory lettuceConnectionFactory;
    private final RedisFailOpenGate gate;

    @Scheduled(fixedDelayString = "${redis.fail-open.probe-interval-ms:2000}")
    public void probe() {
        if (!gate.isOpen()) {
            return;
        }
        try (var connection = lettuceConnectionFactory.getConnection()) {
            if ("PONG".equalsIgnoreCase(connection.ping())) {
                gate.onSuccess();
                log.info("Redis fail-open closed after successful ping");
            } else {
                gate.onFailure();
            }
        } catch (Exception exception) {
            log.warn("Redis fail-open probe failed: {}", exception.getMessage());
            gate.onFailure();
        }
    }
}
