package com.winten.greenlight.prototype.core.api.controller.queue;

import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueControllerTest {

    @InjectMocks
    private QueueController queueController;

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private QueueService queueService;

    @Mock
    private ReactiveZSetOperations<String, String> zSetOps;

    private WebTestClient webTestClient;

    private static final String ACTIVE_USERS_KEY = "activeUsers";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.delete(anyString())).thenReturn(Mono.just(true));
        webTestClient = WebTestClient.bindToController(queueController).build();
    }

    @Test
    void testEnterQueue() {
        // Given
        String actionId = "event123";
        String queueId = java.util.UUID.randomUUID().toString();
        String requestBody = "{\"actionId\":\"" + actionId + "\"}";

        when(queueService.enterQueue(eq(actionId))).thenReturn(Mono.just(queueId));
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));

        // When
        webTestClient.post()
            .uri("/queue/enter")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(response -> {
                String responseToken = response.getResponseBody();
                assertNotNull(responseToken, "Token should not be null");
                assertTrue(responseToken.equals(queueId), "Queue ID should match");
            });
    }

    @Test
    void testHeartbeatWithValidToken() {
        // Given
        String actionId = "event123";
        String queueId = java.util.UUID.randomUUID().toString();
        long score = System.currentTimeMillis();
        String token = "valid.token";

        when(jwtUtil.validateToken(token)).thenReturn(mock(com.auth0.jwt.interfaces.DecodedJWT.class));
        when(jwtUtil.validateToken(token).getSubject()).thenReturn(queueId);
        when(queueService.heartbeat(eq(queueId))).thenReturn(Mono.empty());
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));
        when(zSetOps.score(anyString(), anyString())).thenReturn(Mono.just((double) (score + 1000)));

        // When
        webTestClient.post()
            .uri("/queue/heartbeat")
            .cookie("queueToken", token)
            .exchange()
            // Then
            .expectStatus().isOk();
    }

    @Test
    void testHeartbeatWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        when(jwtUtil.validateToken(invalidToken)).thenThrow(new RuntimeException("Invalid or expired token"));

        // When
        webTestClient.post()
            .uri("/queue/heartbeat")
            .cookie("queueToken", invalidToken)
            .exchange()
            // Then
            .expectStatus().isUnauthorized()
            .expectBody(String.class)
            .isEqualTo("Invalid or expired token");
    }

    @Test
    void testHeartbeatWithoutToken() {
        // When
        webTestClient.post()
            .uri("/queue/heartbeat")
            .exchange()
            // Then
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .isEqualTo("Missing queueToken cookie");
    }
}

