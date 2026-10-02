package com.winten.greenlight.core.support.redis;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RedisFailOpenGate {
    static final int THRESHOLD = 3;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicBoolean open = new AtomicBoolean();

    public boolean isOpen() {
        return open.get();
    }

    public void onSuccess() {
        consecutiveFailures.set(0);
        open.set(false);
    }

    public void onFailure() {
        if (consecutiveFailures.incrementAndGet() >= THRESHOLD) {
            open.set(true);
        }
    }
}
