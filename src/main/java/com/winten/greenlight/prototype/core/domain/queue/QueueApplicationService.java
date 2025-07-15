
package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.domain.action.ActionDomainService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.customer.EntryTicket;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.RuleMatcher;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 대기열 시스템의 핵심 애플리케이션 서비스입니다.
 * 클라이언트의 요청을 받아 여러 도메인 서비스(Action, Queue)를 조율하여
 * 대기열 적용 여부를 판단하고 상태를 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class QueueApplicationService {

    private final ActionDomainService actionDomainService;
    private final QueueDomainService queueDomainService;
    private final RuleMatcher ruleMatcher;
    private final TokenDomainService tokenDomainService;

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param hasToken      사용자가 유효한 토큰을 가지고 있는지 여부
     * @param requestParams 사용자가 요청한 모든 쿼리 파라미터 맵 (ActionRule 검사에 사용)
     * @return Mono<EntryTicket> 대기 상태 및 토큰 정보
     */
    public Mono<EntryTicket> checkOrEnterQueue(Long actionId, boolean hasToken, Map<String, String> requestParams) {
        return actionDomainService.findActionById(actionId)
            .switchIfEmpty(Mono.error(new CoreException(ErrorType.ACTION_NOT_FOUND, "Action not found for ID: " + actionId)))
            .flatMap(action ->
                actionDomainService.findRulesByActionId(action.getId())
                    .collectList()
                    .flatMap(rules -> {
                        // 1. 큐 적용 대상이 아닌 경우, BYPASSED 처리
                        if (!ruleMatcher.isRequestSubjectToQueue(action, rules, requestParams)) {
                            return Mono.just(new EntryTicket(WaitStatus.BYPASSED, null));
                        }

                        // 2. 액션이 비활성화된 경우, DISABLED 처리
                        return actionDomainService.isActionEffectivelyEnabled(action)
                            .flatMap(isEnabled -> {
                                if (!isEnabled) {
                                    return Mono.just(new EntryTicket(WaitStatus.DISABLED, null));
                                }

                                // 3. 유효한 토큰을 이미 가지고 있는 경우, READY 처리 (새 토큰 발급 없음)
                                if (hasToken) {
                                    return Mono.just(new EntryTicket(WaitStatus.READY, null));
                                }

                                // 4. 토큰이 없는 신규 진입자 처리
                                final String customerId = requestParams.get("customerId");
                                if (customerId == null || customerId.trim().isEmpty()) {
                                    return Mono.error(new CoreException(ErrorType.BAD_REQUEST, "customerId is required in requestParams for new entries."));
                                }

                                return queueDomainService.isWaitingRequired(action.getActionGroupId())
                                    .flatMap(isWaiting -> {
                                        if (isWaiting) {
                                            // 5. 대기가 필요한 경우: WAITING 토큰 발급 및 대기열 등록
                                            return tokenDomainService.issueToken(customerId, action, WaitStatus.WAITING.name())
                                                .flatMap(newJwt ->
                                                    queueDomainService.addUserToQueue(action.getId(), customerId)
                                                        .thenReturn(new EntryTicket(WaitStatus.WAITING, newJwt))
                                                );
                                        } else {
                                            // 6. 대기가 필요 없는 경우: READY 토큰 발급
                                            return tokenDomainService.issueToken(customerId, action, WaitStatus.READY.name())
                                                .map(newJwt -> new EntryTicket(WaitStatus.READY, newJwt));
                                        }
                                    });
                            });
                    })
            );
    }