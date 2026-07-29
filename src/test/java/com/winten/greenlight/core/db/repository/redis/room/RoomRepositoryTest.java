package com.winten.greenlight.core.db.repository.redis.room;

import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomRepositoryTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, Object> jsonRedisTemplate;
    @Mock
    private ReactiveHashOperations<String, String, Object> hashOperations;
    @Mock
    private RoomRedisScript roomRedisScript;
    @Mock
    private RedisKeyBuilder keyBuilder;
    @Mock
    private JsonMapper jsonMapper;

    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        roomRepository = new RoomRepository(
                redisTemplate,
                jsonRedisTemplate,
                roomRedisScript,
                keyBuilder,
                jsonMapper
        );
        when(jsonRedisTemplate.<String, Object>opsForHash()).thenReturn(hashOperations);
        when(keyBuilder.siteInfoMeta("site-1")).thenReturn("greenlight:site:site-1:meta");
    }

    @Test
    void readsCurrentSiteAndQueueFlagsWithOneHmget() {
        when(hashOperations.multiGet(
                "greenlight:site:site-1:meta",
                List.of("siteEnabled", "queueEnabled")
        )).thenReturn(Mono.just(List.of(false, true)));

        StepVerifier.create(roomRepository.findSiteOperationStatus("site-1"))
                .assertNext(status -> {
                    assertThat(status.siteEnabled()).isFalse();
                    assertThat(status.queueEnabled()).isTrue();
                })
                .verifyComplete();

        verify(hashOperations).multiGet(
                "greenlight:site:site-1:meta",
                List.of("siteEnabled", "queueEnabled")
        );
    }

    @Test
    void treatsLegacySiteEnabledAsQueueEnabledWhenQueueFlagIsAbsent() {
        when(hashOperations.multiGet(
                "greenlight:site:site-1:meta",
                List.of("siteEnabled", "queueEnabled")
        )).thenReturn(Mono.just(Arrays.asList(false, null)));

        StepVerifier.create(roomRepository.findSiteOperationStatus("site-1"))
                .assertNext(status -> {
                    assertThat(status.siteEnabled()).isTrue();
                    assertThat(status.queueEnabled()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void rejectsWhenSiteMetaHashIsAbsent() {
        when(hashOperations.multiGet(
                "greenlight:site:site-1:meta",
                List.of("siteEnabled", "queueEnabled")
        )).thenReturn(Mono.just(Arrays.asList(null, null)));

        StepVerifier.create(roomRepository.findSiteOperationStatus("site-1"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(CoreException.class);
                    assertThat(((CoreException) error).getErrorCode()).isEqualTo(ErrorCode.SITE_STATUS_UNAVAILABLE);
                })
                .verify();
    }
}
