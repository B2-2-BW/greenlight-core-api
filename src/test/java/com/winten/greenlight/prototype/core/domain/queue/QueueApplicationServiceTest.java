
package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.api.controller.queue.dto.CheckOrEnterResponse;
import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.action.ActionDomainService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

// LOKI_URL 플레이스홀더 문제를 해결하기 위해 테스트용 프로퍼티를 추가합니다.
@TestPropertySource(properties = { "LOKI_URL=http://localhost:3100/loki/api/v1/push" })
@SpringBootTest(classes = {QueueApplicationService.class, QueueApplicationServiceTest.TestConfig.class})
@DisplayName("QueueApplicationService 통합 테스트 (Final Fixed)")
class QueueApplicationServiceTest {

    @Autowired
    private QueueApplicationService queueApplicationService;

    @Autowired
    private ActionDomainService actionDomainService;
    @Autowired
    private QueueDomainService queueDomainService;
    @Autowired
    private TokenDomainService tokenDomainService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ActionDomainService actionDomainService() {
            return Mockito.mock(ActionDomainService.class);
        }

        @Bean
        public QueueDomainService queueDomainService() {
            return Mockito.mock(QueueDomainService.class);
        }

        @Bean
        public TokenDomainService tokenDomainService() {
            return Mockito.mock(TokenDomainService.class);
        }
    }

    private static final String TEST_ACTION_URL = "/products/limited-edition";
    private static final String TEST_CUSTOMER_ID = "customer-123";
    private static final Long TEST_ACTION_ID = 1L;
    private static final Long TEST_ACTION_GROUP_ID = 10L;

    private Action testAction;

    @BeforeEach
    void setUp() {
        testAction = new Action();
        testAction.setId(TEST_ACTION_ID);
        testAction.setActionGroupId(TEST_ACTION_GROUP_ID);
        testAction.setActionUrl(TEST_ACTION_URL);
    }

    @Test
    @DisplayName("신규 사용자 & 대기 불필요 시, ALLOWED 토큰을 발급한다")
    void checkOrEnterQueue_NewUser_NoWaitRequired() {
        // given
        when(actionDomainService.findActionByUrl(TEST_ACTION_URL)).thenReturn(Mono.just(testAction));
        when(actionDomainService.isActionEffectivelyEnabled(testAction)).thenReturn(Mono.just(true));
        when(tokenDomainService.findValidTokenJwt(TEST_CUSTOMER_ID, TEST_ACTION_ID)).thenReturn(Mono.empty());
        when(queueDomainService.isWaitingRequired(TEST_ACTION_GROUP_ID)).thenReturn(Mono.just(false));
        when(tokenDomainService.issueToken(TEST_CUSTOMER_ID, testAction, WaitStatus.ALLOWED.name()))
            .thenReturn(Mono.just("new-allowed-token"));

        // when
        Mono<CheckOrEnterResponse> result = queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID);

        // then
        StepVerifier.create(result)
            .expectNextMatches(response ->
                response.getStatus().equals(WaitStatus.ALLOWED.name()) &&
                    response.getToken().equals("new-allowed-token") &&
                    response.getRank() == 0L
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("신규 사용자 & 대기 필요 시, WAITING 토큰을 발급하고 대기열에 추가한다")
    void checkOrEnterQueue_NewUser_WaitRequired() {
        // given
        when(actionDomainService.findActionByUrl(TEST_ACTION_URL)).thenReturn(Mono.just(testAction));
        when(actionDomainService.isActionEffectivelyEnabled(testAction)).thenReturn(Mono.just(true));
        when(tokenDomainService.findValidTokenJwt(TEST_CUSTOMER_ID, TEST_ACTION_ID)).thenReturn(Mono.empty());
        when(queueDomainService.isWaitingRequired(TEST_ACTION_GROUP_ID)).thenReturn(Mono.just(true));
        when(tokenDomainService.issueToken(TEST_CUSTOMER_ID, testAction, WaitStatus.WAITING.name()))
            .thenReturn(Mono.just("new-waiting-token"));
        when(queueDomainService.addUserToQueue(eq(TEST_ACTION_ID), anyString())).thenReturn(Mono.just(15L));

        // when
        Mono<CheckOrEnterResponse> result = queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID);

        // then
        StepVerifier.create(result)
            .expectNextMatches(response ->
                response.getStatus().equals(WaitStatus.WAITING.name()) &&
                    response.getToken().equals("new-waiting-token") &&
                    response.getRank() == 15L
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("기존 토큰 보유 시, EXISTING 상태와 기존 토큰, 현재 순번을 반환한다")
    void checkOrEnterQueue_ExistingUser() {
        // given
        String existingToken = "existing-token";
        when(actionDomainService.findActionByUrl(TEST_ACTION_URL)).thenReturn(Mono.just(testAction));
        when(actionDomainService.isActionEffectivelyEnabled(testAction)).thenReturn(Mono.just(true));
        when(tokenDomainService.findValidTokenJwt(TEST_CUSTOMER_ID, TEST_ACTION_ID)).thenReturn(Mono.just(existingToken));
        when(queueDomainService.getQueueRank(eq(TEST_ACTION_ID), anyString())).thenReturn(Mono.just(5L));

        // when
        Mono<CheckOrEnterResponse> result = queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID);

        // then
        StepVerifier.create(result)
            .expectNextMatches(response ->
                response.getStatus().equals("EXISTING") &&
                    response.getToken().equals(existingToken) &&
                    response.getRank() == 5L
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Action이 비활성화된 경우, DISABLED 상태를 반환한다")
    void checkOrEnterQueue_ActionDisabled() {
        // given
        when(actionDomainService.findActionByUrl(TEST_ACTION_URL)).thenReturn(Mono.just(testAction));
        when(actionDomainService.isActionEffectivelyEnabled(testAction)).thenReturn(Mono.just(false));

        // when
        Mono<CheckOrEnterResponse> result = queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID);

        // then
        StepVerifier.create(result)
            .expectNextMatches(response -> response.getStatus().equals("DISABLED"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Action을 찾을 수 없는 경우, ACTION_NOT_FOUND 상태를 반환한다")
    void checkOrEnterQueue_ActionNotFound() {
        // given
        when(actionDomainService.findActionByUrl(TEST_ACTION_URL)).thenReturn(Mono.empty());

        // when
        Mono<CheckOrEnterResponse> result = queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID);

        // then
        StepVerifier.create(result)
            .expectNextMatches(response -> response.getStatus().equals("ACTION_NOT_FOUND"))
            .verifyComplete();
    }
}
