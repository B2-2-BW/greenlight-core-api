package com.winten.greenlight.core.support.cache;

import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LocalCacheConfig {
    public static final Duration META_EXPIRE_AFTER_WRITE = Duration.ofSeconds(1);
    public static final Duration META_REFRESH_AFTER_WRITE = Duration.ofMillis(500);
    public static final int META_MAXIMUM_SIZE = 10_000;
}