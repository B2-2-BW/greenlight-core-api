package com.winten.greenlight.core.support.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class LocalCacheConfig {
    @Bean // 기본 로컬캐싱 설정
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}