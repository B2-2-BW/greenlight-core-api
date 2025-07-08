
package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.api.controller.queue.dto.CheckOrEnterResponse;
import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.action.ActionDomainService;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
import com.winten.greenlight.prototype.core.support.util.RuleMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueApplicationService {

    private final ActionDomainService actionDomainService;
    private final QueueDomainService queueDomainService;
    private final TokenDomainService tokenDomainService;
    // ActionRule의 matchOperator를 해석하고 비교하는 로직을 담당할 헬퍼 클래스 (가정)
    private final RuleMatcher ruleMatcher; // 이 클래스는 새로 만들어야 할 수 있습니다.

    /**
     * 사용자의 대기열 진입 요청을 처리하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     * (리팩토링) 이제 actionUrl 외에 requestParams를 받아 ActionRule을 검사합니다.
     *
     * @param actionUrl     사용자가 접근하려는 액션의 URL
     * @param customerId    고객 ID
     * @param requestParams 사용자가 요청한 모든 쿼리 파라미터 맵
     * @return Mono<CheckOrEnterResponse> 대기열 진입 결과 응답
     */
    public Mono<CheckOrEnterResponse> checkOrEnterQueue(String actionUrl, String customerId, Map<String, String> requestParams) {
        return actionDomainService.findActionByUrl(actionUrl)
            .flatMap(action ->
                actionDomainService.findRulesByActionId(action.getId())
                    .collectList()
                    .flatMap(rules -> {
                        if (!ruleMatcher.isRequestSubjectToQueue(action, rules, requestParams)) {
                            return issueNewAllowedToken(customerId, action, "BYPASSED_BY_RULE");
                        }
                        return actionDomainService.isActionEffectivelyEnabled(action)
                            .flatMap(isEnabled -> {
                                if (!isEnabled) return Mono.just(new CheckOrEnterResponse("DISABLED", null, null));
                                return tokenDomainService.findValidTokenJwt(customerId, action.getId())
                                    .flatMap(jwt -> queueDomainService.getQueueRank(action.getId(), jwt).map(rank -> new CheckOrEnterResponse("EXISTING", jwt, rank)))
                                    .switchIfEmpty(Mono.defer(() -> queueDomainService.isWaitingRequired(action.getActionGroupId())
                                        .flatMap(isWaiting -> isWaiting ? issueNewWaitingToken(customerId, action) : issueNewAllowedToken(customerId, action, WaitStatus.ALLOWED.name()))));
                            });
                    }))
            .switchIfEmpty(Mono.just(new CheckOrEnterResponse("ACTION_NOT_FOUND", null, null)));
    }

    private Mono<CheckOrEnterResponse> issueNewWaitingToken(String customerId, Action action) {
        return tokenDomainService.issueToken(customerId, action, WaitStatus.WAITING.name())
            .flatMap(newJwt -> queueDomainService.addUserToQueue(action.getId(), newJwt)
                .map(rank -> new CheckOrEnterResponse(WaitStatus.WAITING.name(), newJwt, rank)));
    }

    private Mono<CheckOrEnterResponse> issueNewAllowedToken(String customerId, Action action, String status) {
        return tokenDomainService.issueToken(customerId, action, WaitStatus.ALLOWED.name())
            .map(newJwt -> new CheckOrEnterResponse(status, newJwt, 0L));
    }

    private String jwtToQueueId(String jwt) {
        return "queueId-from-" + jwt;
    }
}
