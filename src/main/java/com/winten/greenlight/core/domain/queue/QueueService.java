package com.winten.greenlight.core.domain.queue;

import com.winten.greenlight.core.api.controller.customer.TicketVerificationResponse;
import com.winten.greenlight.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.domain.action.Action;
import com.winten.greenlight.core.domain.action.ActionGroup;
import com.winten.greenlight.core.domain.action.CachedActionService;
import com.winten.greenlight.core.domain.customer.CustomerSession;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 대기열 시스템의 핵심 애플리케이션 서비스입니다.
 * 클라이언트의 요청을 받아 여러 도메인 서비스(Action, Queue)를 조율하여
 * 대기열 적용 여부를 판단하고 상태를 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisKeyBuilder redisKeyBuilder;
    private final QueueRepository queueRepository;
    private final ActionRepository actionRepository;
    private final CachedActionService cachedActionService;
    private final ActionEventPublisher actionEventPublisher;
    private final CustomerRepository customerRepository;

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param landingId      사용자가 접근하려는 랜딩 ID
     * @param greenlightId (Optional) 고객이 보유한 대기열 토큰
     * @return Mono<CustomerSession> 대기 상태 및 토큰 정보
     */
    public Mono<CustomerSession> checkLanding(String landingId, String destinationUrl, String greenlightId) {
        return cachedActionService.getActionByLandingId(landingId)
                .flatMap(action -> checkOrEnterQueue(action.getId(), destinationUrl != null ? destinationUrl : action.getLandingDestinationUrl(), greenlightId))
                .switchIfEmpty(Mono.just(CustomerSession.bypassed()));
    }

    public Mono<CustomerSession> checkOrEnterQueue(Long actionId, String destinationUrl, String oldCustomerId) {
        return cachedActionService.getActionById(actionId)
            .switchIfEmpty(Mono.error(new CoreException(ErrorType.ACTION_NOT_FOUND, "Action not found for ID: " + actionId)))
            .flatMap(action -> {
                // 2. 액션이 비활성화된 경우, DISABLED 처리
                return cachedActionService.getActionGroupById(action.getActionGroupId())
                    .flatMap(actionGroup -> {
                        if (!actionGroup.getEnabled()) {
                            return Mono.just(CustomerSession.bypassed());
                        }

                        String customerKey = null;
                        try { // 기존에 사용하던 토큰이 있는 경우 customerId의 고유번호 추출
                            customerKey = oldCustomerId.split(":")[1];
                        } catch (Exception ignored) {}
                        return handleNewEntry(actionGroup, action, destinationUrl, customerKey);
                    });
            });
    }

    /**
     * 토큰이 없거나 유효하지 않은 경우, 혹은 다른 actionId의 토큰인 경우 신규 진입자를 처리합니다.
     * customerId를 생성하고 대기열 필요 여부에 따라 WAITING 또는 READY 큐에 추가합니다.
     *
     * @param destinationUrl 목적지 URL
     * @param action Action 객체
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    private Mono<CustomerSession> handleNewEntry(ActionGroup actionGroup, Action action, String destinationUrl, String oldCustomerKey) {
        String customerKey = oldCustomerKey != null ? oldCustomerKey : generateCustomerKey();
        String customerId = makeCustomerId(action.getId(), customerKey);
        return isWaitingRequired(actionGroup) // 현재 대기가 필요한 상황인지 확인
                .flatMap(isWaitingRequired -> {
                    // 대기가 필요한 경우 status = WAITING
                    // 대기없이 바로 입장이 가능한 경우 status = READY
                    WaitStatus status = isWaitingRequired ? WaitStatus.WAITING : WaitStatus.READY;
                    var now = System.currentTimeMillis();
                    var session = CustomerSession.builder()
                            .actionId(action.getId())
                            .actionGroupId(actionGroup.getId())
                            .customerId(customerId)
                            .verified(false)
                            .accessCount(0L)
                            .destinationUrl(destinationUrl)
                            .timestamp(now)
                            .waitStatus(status)
                            .build();

                    return addUserToQueue(action.getActionGroupId(), customerId, status)
                            .then(customerRepository.saveCustomerSession(session, Duration.ofDays(1)))
                            .then(actionEventPublisher.publish(status, action.getActionGroupId(), action.getId(), customerId, now)) // influxDB에 현재 이벤트 기록 (대기, 입장준비 등)
                            .then(actionRepository.putRequestLog(actionGroup.getId(), customerId)) // 활성사용자수 계산을 위한 접속기록 로깅
                            .then(actionRepository.putSession(customerKey)) // 5분 동시접속자 수 계산을 위한 로깅
                            .thenReturn(session);

                });
    }

    public String generateCustomerKey() {
        return TSID.fast().toString();
    }
    /**
     * actionId를 기반으로 고유한 customerId를 생성합니다.
     * customerId는 {actionId}:{tsid} 형식입니다.
     *
     * @param actionId 액션 ID
     * @return Mono<String> 생성된 customerId
     */
    private String makeCustomerId(Long actionId, String customerKey) {
        return actionId + ":" + customerKey;
    }

    /**
     * 특정 ActionGroup에 대해 현재 대기가 필요한지 판단합니다.
     * 활성 사용자 수 및 대기고객수가 ActionGroup의 최대 허용 고객 수를 초과하는지 확인합니다.
     * 활성 사용자 수 계산 시 3초 평균 사용자수를 측정합니다.
     *
     * @param actionGroup 검사할 ActionGroup
     * @return Mono<Boolean> 대기 필요 여부
     */
    private Mono<Boolean> isWaitingRequired(ActionGroup actionGroup) {
        // T1 = 대기고객 수, T2 = 활성사용자 수
        // 대기고객이 있는 경우 무조건 웨이팅
        // 대기고객이 없는 경우 활성사용자수가 최대 활성사용자수보다 적으면 입장 가능
        return Mono.zip(actionRepository.getWaitingCountByActionGroupId(actionGroup.getId()),
                        queueRepository.getCurrentRequestPerSec(actionGroup.getId()))
                .map(tuple -> tuple.getT1() > 0 || (tuple.getT2()) >= (double) actionGroup.getMaxTrafficPerSecond());
    }

    /**
     * 사용자를 지정된 상태의 대기열(Redis Sorted Set)에 추가합니다.
     *
     * @param actionGroupId 사용자가 진입하려는 ActionGroup의 ID
     * @param customerId  사용자에게 부여된 고유 ID
     * @param status 대기열의 상태 (WAITING or READY)
     * @return Mono<Long> 대기열에 추가된 후의 순번 (0부터 시작)
     */
    private Mono<Boolean> addUserToQueue(Long actionGroupId, String customerId, WaitStatus status) {
        String queueKey = redisKeyBuilder.queue(actionGroupId, status);
        return queueRepository.add(queueKey, customerId, System.currentTimeMillis())
                .map(count -> count > 0);
    }

    /**
     * Customer ID로 현재 세션을 조회
      */
    public Mono<TicketVerificationResponse> verifyTicket(String customerId) {
        return customerRepository.getCustomerSessionById(customerId)
                .map(customerSession -> {
                    var now = System.currentTimeMillis();
                    var waitTimeMs = now - customerSession.getTimestamp(); // 고객의 score와 지금 시간차만큼 대기한 것으로 판단
                    customerSession.setTimestamp(now);
                    customerSession.setWaitTimeMs(waitTimeMs);
                    return customerSession;
                })
                .flatMap(customer -> Mono.zip(
                            customerRepository.isCustomerReady(customer.getActionGroupId(), customerId).switchIfEmpty(Mono.just(false)), // T1 대기 완료여부 확인
                            customerRepository.findCustomerVerifiedFromSession(customerId).switchIfEmpty(Mono.just(false))
                        )
                        .flatMap(tuple -> {
                            if (tuple.getT2()) { // (verified == true) 이미 대기열에 한번 입장했던 고객인 경우 바로 입장 TODO (POC 기간 한시적으로 적용)
                                return Mono.just(TicketVerificationResponse.success(customer));
                            }
                            if (!tuple.getT1()) { // 예외케이스, 대기가 완료되지 않은 경우
                                return Mono.just(TicketVerificationResponse.fail(customerId, "대기 ID가 유효하지 않거나 대기가 완료되지 않았습니다.")); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                            }
                            return customerRepository.updateSessionVerified(customerId, true) // 세션의 대기상태를 ENTERED로 변경
                                    .then(customerRepository.increaseSessionAccessCount(customerId, 1L)) // Timestamp도 변경, TODO 위에 success가 먼저 타서 +1이 안됨
                                    .then(Mono.defer(() -> {
                                        customer.setWaitStatus(WaitStatus.ENTERED);
                                        return actionEventPublisher.publish(customer);
                                    }))
                                    .then(actionRepository.putAccessLog(customer.getActionGroupId(), customer.getCustomerId()))
                                    .then(actionRepository.putSession(customer.uniqueId()))
                                    .then(customerRepository.deleteCustomer(customer.getActionGroupId(), customer.getCustomerId(), WaitStatus.READY)
                                               .map(deleted -> deleted ?
                                                       TicketVerificationResponse.success(customer) :
                                                       TicketVerificationResponse.fail(customerId, "Ready 상태를 찾을 수 없습니다.")
                                               )
                                    );
                        })
                )
                .switchIfEmpty(Mono.just(TicketVerificationResponse.fail(customerId, "확인되지 않은 오류입니다.")))
                .onErrorResume(e -> {
                    if (e instanceof CoreException) {
                        return Mono.error(e);
                    } else {
                        return Mono.error(CoreException.of(ErrorType.INTERNAL_SERVER_ERROR, "입장에 실패하였습니다 " + e));
                    }
                });

    }
}