package com.winten.greenlight.core.support.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@EnableCaching
@Configuration
public class LocalCacheConfig {
//    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAsyncCacheMode(true);

        cacheManager.setCaffeine(  // 캐시 기본 지속시간 5분
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofMinutes(1))
        );
        cacheManager.registerCustomCache( // action version 캐시는 영구 지속되도록, 로컬/실제 version 비교용
                "actionVersionCache",
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .build()
        );
        cacheManager.registerCustomCache( // 설정 Cache 는 1일 지속되도록
                "configCache",
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofDays(1))
                        .build()
        );
        return cacheManager;
    }
}