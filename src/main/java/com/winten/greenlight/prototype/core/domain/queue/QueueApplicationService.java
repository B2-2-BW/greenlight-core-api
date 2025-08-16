package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.action.ActionGroup;
import com.winten.greenlight.prototype.core.domain.action.CachedActionService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.customer.EntryTicket;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.publisher.ActionEventPublisher;
import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import com.winten.greenlight.prototype.core.support.util.RuleMatcher;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
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
public class QueueApplicationService {

    private final QueueDomainService queueDomainService;
    private final RuleMatcher ruleMatcher;
    private final TokenDomainService tokenDomainService;
    private final CachedActionService cachedActionService;
    private final ActionEventPublisher actionEventPublisher;
    private final JwtUtil jwtUtil;

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param greenlightToken (Optional) 고객이 보유한 대기열 토큰
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
        public Mono<EntryTicket> checkOrEnterQueue(Long actionId, String destinationUrl, String greenlightToken) {
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
                            customerKey = jwtUtil.getCustomerFromToken(greenlightToken).getCustomerId().split(":")[1];
                        } catch (Exception ignored) {}
                        System.out.println("고객ID: " + customerKey);
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
        return queueDomainService.isWaitingRequired(actionGroup)
                    .flatMap(isWaitingRequired -> {
                        WaitStatus status = isWaitingRequired ? WaitStatus.WAITING : WaitStatus.READY;
                        // 5. 대기가 필요한 경우: WAITING 토큰 발급 및 대기열 등록
                        return tokenDomainService.issueToken(customerId, action, status.name(), destinationUrl)
                                .flatMap(newJwt ->
                                    queueDomainService.addUserToQueue(action.getActionGroupId(), customerId, status)
                                        .then(Mono.defer(() -> {
                                            var returnStatus = status == WaitStatus.WAITING ? WaitStatus.WAITING : WaitStatus.BYPASSED;
                                            return actionEventPublisher.publish(returnStatus, action.getActionGroupId(), action.getId(), customerId, System.currentTimeMillis());
                                        }))
                                        .thenReturn(new EntryTicket(action.getId(), customerId, destinationUrl, System.currentTimeMillis(), status, newJwt))
                                );
                    });
    }

    /**
     * actionId를 기반으로 고유한 customerId를 생성합니다.
     * customerId는 {actionId}:{tsid} 형식입니다.
     *
     * @param actionId 액션 ID
     * @param uniqueKey 랜덤 생성된 고유 문자열 (tsid 기반)
     * @return Mono<String> 생성된 customerId
     */
    public String generateCustomerId(Long actionId, String customerKey) {
        return actionId + ":" + customerKey;
    }

    public String generateCustomerId(Long actionId) {
        return this.generateCustomerId(actionId, TSID.fast().toString());
    }
}