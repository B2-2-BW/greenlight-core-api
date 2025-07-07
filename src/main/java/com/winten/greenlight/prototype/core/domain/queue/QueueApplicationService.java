package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.api.controller.queue.dto.CheckOrEnterResponse;
import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.action.ActionDomainService;

import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.domain.token.TokenDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 대기열 시스템의 핵심 애플리케이션 서비스입니다.
 * 클라이언트의 요청을 받아 여러 도메인 서비스(Action, Queue, Token)를 조율하여
 * 대기열 진입 및 토큰 발급과 관련된 비즈니스 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class QueueApplicationService {

    private final ActionDomainService actionDomainService;
    private final QueueDomainService queueDomainService;
    private final TokenDomainService tokenDomainService;

    /**
     * 사용자의 대기열 진입 요청을 처리하고, 현재 상태에 따라 적절한 응답을 반환합니다.
     *
     * @param actionUrl  사용자가 접근하려는 액션의 URL
     * @param customerId 고객 ID
     * @return Mono<CheckOrEnterResponse> 대기열 진입 결과 응답
     */
    public Mono<CheckOrEnterResponse> checkOrEnterQueue(String actionUrl, String customerId) {
        // 1. actionUrl을 기반으로 Action 정보를 조회.
        return actionDomainService.findActionByUrl(actionUrl)
            .flatMap(action ->
                // 2. 조회된 Action이 실질적으로 활성화되어 있는지 확인.
                actionDomainService.isActionEffectivelyEnabled(action)
                    .flatMap(isEnabled -> {
                        if (!isEnabled) {
                            // 2a. Action이 비활성화된 경우, DISABLED 상태를 반환.
                            return Mono.just(new CheckOrEnterResponse("DISABLED", null, null));
                        }

                        // 3. 고객이 이미 유효한 토큰을 가지고 있는지 확인.
                        return tokenDomainService.findValidTokenJwt(customerId, action.getId())
                            .flatMap(existingJwt ->
                                // 3a. 유효한 토큰이 있는 경우, 해당 토큰의 현재 대기 순번을 조회하여 반환.
                                queueDomainService.getQueueRank(action.getId(), jwtToQueueId(existingJwt))
                                    .map(rank -> new CheckOrEnterResponse("EXISTING", existingJwt, rank)))
                            .switchIfEmpty(Mono.defer(() ->
                                // 3b. 유효한 토큰이 없는 경우, 대기 필요 여부를 판단합니다.
                                queueDomainService.isWaitingRequired(action.getActionGroupId())
                                    .flatMap(isWaiting -> {
                                        if (isWaiting) {
                                            // 4a. 대기가 필요한 경우, WAITING 상태의 토큰을 발급하고 대기열에 추가합니다.
                                            return issueNewWaitingToken(customerId, action);
                                        } else {
                                            // 4b. 즉시 입장 가능한 경우, ALLOWED 상태의 토큰을 발급합니다.
                                            return issueNewAllowedToken(customerId, action);
                                        }
                                    })));
                    }))
            // 5. actionUrl에 해당하는 Action을 찾을 수 없는 경우, ACTION_NOT_FOUND 상태를 반환합니다。
            .switchIfEmpty(Mono.just(new CheckOrEnterResponse("ACTION_NOT_FOUND", null, null)));
    }

    /**
     * 새로운 WAITING 상태의 토큰을 발급하고, 사용자를 대기열에 추가합니다.
     *
     * @param customerId 고객 ID
     * @param action     관련 Action 정보
     * @return Mono<CheckOrEnterResponse> WAITING 상태 응답
     */
    private Mono<CheckOrEnterResponse> issueNewWaitingToken(String customerId, Action action) {
        return tokenDomainService.issueToken(customerId, action, WaitStatus.WAITING.name()) // String으로 전달
            .flatMap(newJwt -> queueDomainService.addUserToQueue(action.getId(), jwtToQueueId(newJwt))
                .map(rank -> new CheckOrEnterResponse(WaitStatus.WAITING.name(), newJwt, rank)));
    }

    /**
     * 새로운 ALLOWED 상태의 토큰을 발급합니다. (즉시 입장 가능)
     *
     * @param customerId 고객 ID
     * @param action     관련 Action 정보
     * @return Mono<CheckOrEnterResponse> ALLOWED 상태 응답
     */
    private Mono<CheckOrEnterResponse> issueNewAllowedToken(String customerId, Action action) {
        return tokenDomainService.issueToken(customerId, action, WaitStatus.ALLOWED.name()) // String으로 전달
            .map(newJwt -> new CheckOrEnterResponse(WaitStatus.ALLOWED.name(), newJwt, 0L));
    }

    /**
     * JWT 토큰에서 queueId (또는 고객 식별자)를 추출합니다.
     * TODO: 실제 JwtUtil을 사용하여 토큰 파싱 로직을 구현해야 합니다.
     *
     * @param jwt JWT 토큰 문자열
     * @return 추출된 queueId 문자열
     */
    private String jwtToQueueId(String jwt) {
        // 실제로는 JwtUtil을 사용하여 subject 등을 추출해야 합니다.
        return "queueId-from-" + jwt;
    }
}
