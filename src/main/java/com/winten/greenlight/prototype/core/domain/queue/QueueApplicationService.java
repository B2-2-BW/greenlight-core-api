
package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.domain.action.ActionDomainService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import com.winten.greenlight.prototype.core.support.util.RuleMatcher;
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

    /**
     * 사용자의 대기열 상태를 확인하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * 이 메소드는 ActionRule을 검사하여 요청의 대기열 적용 여부를 동적으로 결정합니다.
     *
     * @param actionId      사용자가 접근하려는 액션의 ID
     * @param hasToken      사용자가 유효한 토큰을 가지고 있는지 여부
     * @param requestParams 사용자가 요청한 모든 쿼리 파라미터 맵 (ActionRule 검사에 사용)
     * @return Mono<String> 대기 상태 (WAITING, READY, BYPASSED, DISABLED)
     */
    public Mono<String> checkOrEnterQueue(Long actionId, boolean hasToken, Map<String, String> requestParams) {
        return actionDomainService.findActionById(actionId)
            .switchIfEmpty(Mono.error(new CoreException(ErrorType.ACTION_NOT_FOUND, "Action not found for ID: " + actionId)))
            .flatMap(action ->
                actionDomainService.findRulesByActionId(action.getId())
                    .collectList()
                    .flatMap(rules -> {
                        if (!ruleMatcher.isRequestSubjectToQueue(action, rules, requestParams)) {
                            return Mono.just("BYPASSED_BY_RULE");
                        }

                        return actionDomainService.isActionEffectivelyEnabled(action)
                            .flatMap(isEnabled -> {
                                if (!isEnabled) {
                                    return Mono.just("DISABLED");
                                }

                                // 토큰이 있는 사용자는 항상 READY 상태로 간주합니다.
                                if (hasToken) {
                                    return Mono.just(WaitStatus.READY.name());
                                }

                                return queueDomainService.isWaitingRequired(action.getActionGroupId())
                                    .flatMap(isWaiting -> {
                                        if (isWaiting) {
                                            // 토큰이 없는 사용자는 대기가 필요하면 WAITING 상태를 반환합니다.
                                            return Mono.just(WaitStatus.WAITING.name());
                                        } else {
                                            // 토큰이 없는 사용자도 대기가 필요 없으면 READY 상태를 반환합니다.
                                            return Mono.just(WaitStatus.READY.name());
                                        }
                                    });
                            });
                    }));
    }
}