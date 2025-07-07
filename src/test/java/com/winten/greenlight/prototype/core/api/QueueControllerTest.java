package com.winten.greenlight.prototype.core.api;
import com.winten.greenlight.prototype.core.api.controller.queue.QueueController;
import com.winten.greenlight.prototype.core.api.controller.queue.dto.CheckOrEnterResponse;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.queue.QueueApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(QueueController.class) // 정확한 컨트롤러를 테스트 대상으로 지정
@DisplayName("QueueController - 대기열 진입/확인 API 테스트")
class QueueControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean // Controller의 유일한 의존성인 ApplicationService만 Mocking
    private QueueApplicationService queueApplicationService;

    private static final String TEST_ACTION_URL = "/products/limited-edition";
    private static final String TEST_CUSTOMER_ID = "customer-123";

    @Test
    @DisplayName("대기가 필요 없을 경우, ALLOWED 상태와 토큰을 반환한다")
    void checkOrEnterQueue_whenNoWaitRequired_shouldReturnAllowed() {
        // given: Service가 ALLOWED 상태의 응답을 반환하도록 설정
        String newToken = "new-allowed-token";
        CheckOrEnterResponse serviceResponse = CheckOrEnterResponse.builder()
            .status(WaitStatus.ALLOWED.name())
            .token(newToken)
            .build();

        when(queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID))
            .thenReturn(Mono.just(serviceResponse));

        // when & then: 실제 API를 호출하고 응답을 검증
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/queue/check-or-enter")
                .queryParam("actionUrl", TEST_ACTION_URL)
                .queryParam("customerId", TEST_CUSTOMER_ID)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ALLOWED")
            .jsonPath("$.token").isEqualTo(newToken)
            .jsonPath("$.waitingNumber").doesNotExist(); // 대기 번호는 없어야 함
    }

    @Test
    @DisplayName("대기가 필요할 경우, WAITING 상태와 토큰, 대기번호를 반환한다")
    void checkOrEnterQueue_whenWaitRequired_shouldReturnWaiting() {
        // given: Service가 WAITING 상태의 응답을 반환하도록 설정
        String newToken = "new-waiting-token";
        long waitingNumber = 15L;
        CheckOrEnterResponse serviceResponse = CheckOrEnterResponse.builder()
            .status(WaitStatus.WAITING.name())
            .token(newToken)
            .rank(waitingNumber)
            .build();

        when(queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID))
            .thenReturn(Mono.just(serviceResponse));

        // when & then: 실제 API를 호출하고 응답을 검증
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/queue/check-or-enter")
                .queryParam("actionUrl", TEST_ACTION_URL)
                .queryParam("customerId", TEST_CUSTOMER_ID)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("WAITING")
            .jsonPath("$.token").isEqualTo(newToken)
            .jsonPath("$.waitingNumber").isEqualTo(waitingNumber);
    }

    @Test
    @DisplayName("서비스 로직에서 에러가 발생할 경우, 500 Internal Server Error를 반환한다")
    void checkOrEnterQueue_whenServiceFails_shouldReturn500() {
        // given: Service가 에러를 발생시키도록 설정
        when(queueApplicationService.checkOrEnterQueue(TEST_ACTION_URL, TEST_CUSTOMER_ID))
            .thenReturn(Mono.error(new RuntimeException("Service layer error")));

        // when & then: API 호출 시 500 에러가 발생하는지 검증
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/queue/check-or-enter")
                .queryParam("actionUrl", TEST_ACTION_URL)
                .queryParam("customerId", TEST_CUSTOMER_ID)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError();
    }
}
