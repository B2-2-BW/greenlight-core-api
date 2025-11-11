package com.winten.greenlight.core.domain.queue;

import com.winten.greenlight.core.api.controller.customer.TicketVerificationResponse;
import com.winten.greenlight.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.core.db.repository.redis.customer.CustomerRepository;
import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.domain.action.Action;
import com.winten.greenlight.core.domain.action.ActionGroup;
import com.winten.greenlight.core.domain.action.CachedActionService;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.customer.EntryTicket;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.core.domain.token.TokenService;
import com.winten.greenlight.core.support.util.JwtUtil;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 대기열 시스템의 핵심 애플리케이션 서비스입니다.
 * 클라이언트의 요청을 받아 여러 도메인 서비스(Action, Queue)를 조율하여
 * 대기열 적용 여부를 판단하고 상태를 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisKeyBuilder redisKeyBuilder;
    private final QueueRepository queueRepository;
    private final ActionRepository actionRepository;
    private final TokenService tokenService;
    private final CachedActionService cachedActionService;
    private final ActionEventPublisher actionEventPublisher;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param greenlightId (Optional) 고객이 보유한 대기열 토큰
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
        public Mono<EntryTicket> checkOrEnterQueue(Long actionId, String destinationUrl, String greenlightId) {
            return cachedActionService.getActionById(actionId)
                .switchIfEmpty(Mono.error(new CoreException(ErrorType.ACTION_NOT_FOUND, "Action not found for ID: " + actionId)))
                .flatMap(action -> {
                    // 2. 액션이 비활성화된 경우, DISABLED 처리
                    return cachedActionService.getActionGroupById(action.getActionGroupId())
                        .flatMap(actionGroup -> {
                            if (!actionGroup.getEnabled()) {
                                return Mono.just(new EntryTicket(action.getId(), null, destinationUrl, System.currentTimeMillis(), WaitStatus.DISABLED, null));
                            }

                            String customerKey = null;
                            try { // 기존에 사용하던 토큰이 있는 경우 customerId의 고유번호 추출
                                customerKey = greenlightId.split(":")[1];
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
    private Mono<EntryTicket> handleNewEntry(ActionGroup actionGroup, Action action, String destinationUrl, String customerKey) {
        String customerId = customerKey != null
                                ? generateCustomerId(action.getId(), customerKey) // 기존에 사용중인 고유번호가 있으면 유지
                                : generateCustomerId(action.getId());
        return actionRepository.putRequestLog(actionGroup.getId(), customerId) // 활성사용자수 계산을 위한 접속기록 로깅
                .then(actionRepository.putSession(customerId.split(":")[1])) // 5분 동시접속자 수 계산을 위한 로깅
                .then(isWaitingRequired(actionGroup)
                    .flatMap(isWaitingRequired -> {
                        WaitStatus status = isWaitingRequired ? WaitStatus.WAITING : WaitStatus.READY;
                        // 5. 대기가 필요한 경우: WAITING 토큰 발급 및 대기열 등록
                        return tokenService.issueToken(customerId, action, destinationUrl)
                                .flatMap(newJwt -> // TODO redis에 customerId 기준으로 값들을 저장하니, jwt 쓸 필요가 없어지지 않았나..? == 그냥 세션인듯?
                                    addUserToQueue(action.getActionGroupId(), customerId, status)
                                        .then(Mono.defer(() -> {
                                            var returnStatus = status == WaitStatus.WAITING ? WaitStatus.WAITING : WaitStatus.BYPASSED;
                                            return actionEventPublisher.publish(returnStatus, action.getActionGroupId(), action.getId(), customerId, System.currentTimeMillis());
                                        }))
                                        .then(customerRepository.putCustomerToken(customerId, newJwt)) // 사용자 토큰 저장. TTL = 1일
                                        .thenReturn(new EntryTicket(action.getId(), customerId, destinationUrl, System.currentTimeMillis(), status, newJwt))
                                );
                    })
                );
    }

    /**
     * actionId를 기반으로 고유한 customerId를 생성합니다.
     * customerId는 {actionId}:{tsid} 형식입니다.
     *
     * @param actionId 액션 ID
     * @return Mono<String> 생성된 customerId
     */
    private String generateCustomerId(Long actionId, String customerKey) {
        return actionId + ":" + customerKey;
    }

    private String generateCustomerId(Long actionId) {
        return this.generateCustomerId(actionId, TSID.fast().toString());
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
    private Mono<Long> addUserToQueue(Long actionGroupId, String customerId, WaitStatus status) {
        String queueKey = redisKeyBuilder.queue(actionGroupId, status);
        return queueRepository.add(queueKey, customerId, System.currentTimeMillis());
    }


    public Mono<TicketVerificationResponse> verifyTicket(String greenlightId) {
        return customerRepository.getCustomerTokenById(greenlightId)
                .flatMap(jwt -> {
                    if (!jwtUtil.isTokenValid(jwt)) {
                        return Mono.error(CoreException.of(ErrorType.INVALID_TOKEN, "유효하지 않은 입장권입니다."));
                    }
                    var customer = jwtUtil.getCustomerFromToken(jwt);
                    var now = System.currentTimeMillis();
                    var waitTimeMs = now - customer.getScore(); // 고객의 score와 지금 시간차만큼 대기한 것으로 판단
                    customer.setScore(now);
                    customer.setWaitTimeMs(waitTimeMs);
                    return Mono.just(customer);
                })
                .flatMap(customer -> customerRepository.isCustomerReady(customer)
                        .flatMap(isReady -> {
                            if (!isReady) {
                                return Mono.empty(); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                            }
                            return customerRepository.enqueueCustomer(customer, WaitStatus.ENTERED)
                                    .then(customerRepository.deleteCustomer(customer, WaitStatus.READY))
                                    .then(Mono.defer(() -> { // 모니터링 입장이벤트 로깅
                                        customer.setWaitStatus(WaitStatus.ENTERED);
                                        return actionEventPublisher.publish(customer);
                                    }))
                                    .then(actionRepository.putAccessLog(customer.getActionGroupId(), customer.getCustomerId()))
                                    .then(actionRepository.putSession(customer.uniqueId())) // 5분 동시접속자 수 계산을 위한 로깅
                                    .then(customerRepository.deleteCustomerTokenById(greenlightId))
                                    .then(Mono.just(TicketVerificationResponse.success(customer)));
                        })
                )
                .switchIfEmpty(Mono.just(TicketVerificationResponse.fail(greenlightId, "유효한 고객 ID를 찾을 수 없습니다.")))
                .onErrorResume(e -> {
                    if (e instanceof CoreException) {
                        return Mono.error(e);
                    } else {
                        return Mono.error(CoreException.of(ErrorType.INTERNAL_SERVER_ERROR, "입장에 실패하였습니다 " + e));
                    }
                });

    }

    public Mono<Long> deleteCustomerFromQueue(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            return Mono.error(CoreException.of(ErrorType.INVALID_TOKEN, "유효하지 않은 입장권입니다."));
        }
        var customer = jwtUtil.getCustomerFromToken(token);
        return customerRepository.deleteCustomer(customer, WaitStatus.WAITING)
                .then(customerRepository.deleteCustomer(customer, WaitStatus.READY))
                .then(Mono.defer(() -> {
                    customer.setWaitStatus(WaitStatus.CANCELLED);
                    return actionEventPublisher.publish(customer);
                }))
                .then(Mono.just(1L));
    }
}