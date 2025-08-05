package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.domain.action.CachedActionService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.customer.EntryTicket;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.prototype.core.support.util.RuleMatcher;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 대기열 시스템의 핵심 애플리케이션 서비스입니다.
 * 클라이언트의 요청을 받아 여러 도메인 서비스(Action, Queue)를 조율하여
 * 대기열 적용 여부를 판단하고 상태를 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class QueueApplicationService {

    private final QueueDomainService queueDomainService;
    private final RuleMatcher ruleMatcher;
    private final TokenDomainService tokenDomainService;
    private final CachedActionService cachedActionService;
    private final ActionEventPublisher actionEventPublisher;

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param greenlightToken (Optional) 고객이 보유한 대기열 토큰
     * @param requestParams 사용자가 요청한 모든 쿼리 파라미터 맵 (ActionRule 검사에 사용)
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
        /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param greenlightToken (Optional) 고객이 보유한 대기열 토큰
     * @param requestParams 사용자가 요청한 모든 쿼리 파라미터 맵 (ActionRule 검사에 사용)
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    public Mono<EntryTicket> checkOrEnterQueue(Long actionId, String destinationUrl, String greenlightToken, Map<String, String> requestParams, long timestamp) {
        return cachedActionService.getActionById(actionId)
            .switchIfEmpty(Mono.error(new CoreException(ErrorType.ACTION_NOT_FOUND, "Action not found for ID: " + actionId)))
            .flatMap(action -> cachedActionService.getActionGroupById(action.getActionGroupId())
                .flatMap(actionGroup -> {
                    // 1. 액션 그룹이 비활성화된 경우, DISABLED 처리
                    if (!actionGroup.getEnabled()) {
                        return Mono.just(new EntryTicket(action.getId(), null, destinationUrl, timestamp, WaitStatus.DISABLED, null));
                    }

                    // 2. 토큰 소지 여부에 따라 분기 처리
                    if (StringUtils.hasText(greenlightToken)) {
                        // 2-1. 토큰이 있는 경우: customerId를 유지하고 토큰을 갱신하여 진입 처리
                        String customerId = tokenDomainService.extractCustomerId(greenlightToken);
                        return handleEntry(actionId, action.getActionGroupId(), destinationUrl, action, requestParams, customerId, timestamp);
                    } else {
                        // 2-2. 토큰이 없는 경우: 새로운 customerId를 생성하여 신규 진입 처리
                        return generateCustomerId(actionId)
                            .flatMap(customerId -> handleEntry(actionId, action.getActionGroupId(), destinationUrl, action, requestParams, customerId, timestamp));
                    }
                })
            );
    }

    /**
     * 신규 또는 기존 사용자를 대기열에 진입시킵니다.
     * 대기열 필요 여부에 따라 WAITING 또는 READY 큐에 추가하고, 상태에 맞는 토큰을 발급합니다.
     *
     * @param actionId 액션 ID
     * @param actionGroupId 액션 그룹 ID
     * @param destinationUrl 목적지 URL
     * @param action Action 객체
     * @param requestParams 요청 파라미터 맵
     * @param customerId 사용자 ID (기존 또는 신규)
     * @param timestamp 요청 타임스탬프
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    private Mono<EntryTicket> handleEntry(Long actionId, Long actionGroupId, String destinationUrl, com.winten.greenlight.prototype.core.domain.action.Action action, Map<String, String> requestParams, String customerId, long timestamp) {
        return queueDomainService.isWaitingRequired(actionGroupId)
            .flatMap(isWaiting -> {
                if (isWaiting) {
                    // 3. 대기가 필요한 경우: WAITING 토큰 발급 및 대기열 등록
                    return tokenDomainService.issueToken(customerId, action, WaitStatus.WAITING.name(), destinationUrl)
                        .flatMap(newJwt ->
                            queueDomainService.addUserToQueue(actionGroupId, customerId, WaitStatus.WAITING, timestamp)
                                .then(actionEventPublisher.publish(WaitStatus.WAITING, actionGroupId, actionId, customerId, timestamp))
                                .thenReturn(new EntryTicket(action.getId(), customerId, destinationUrl, timestamp, WaitStatus.WAITING, newJwt))
                        );
                } else {
                    // 4. 대기가 필요 없는 경우: READY 토큰 발급 및 준비열 등록
                    return tokenDomainService.issueToken(customerId, action, WaitStatus.READY.name(), destinationUrl)
                        .flatMap(newJwt ->
                            queueDomainService.addUserToQueue(actionGroupId, customerId, WaitStatus.READY, timestamp)
                                .thenReturn(new EntryTicket(action.getId(), customerId, destinationUrl, timestamp, WaitStatus.READY, newJwt))
                        );
                }
            });
    }

    /**
     * actionId를 기반으로 고유한 customerId를 생성합니다.
     * customerId는 {actionId}:{tsid} 형식입니다.
     *
     * @param actionId 액션 ID
     * @return Mono<String> 생성된 customerId
     */
    public Mono<String> generateCustomerId(Long actionId) {
        return Mono.fromCallable(() -> actionId + ":" + TSID.fast().toString());
    }
}