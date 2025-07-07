package com.winten.greenlight.prototype.core.domain.action;

import com.winten.greenlight.prototype.core.db.repository.redis.action.ActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Action 도메인의 비즈니스 로직을 담당하는 서비스입니다.
 * Action 및 ActionGroup 정보 조회, 활성화 여부 판단 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class ActionDomainService {
    // Action 데이터를 Redis에서 가져오는 CachedActionService를 주입받습니다.
    private final CachedActionService cachedActionService;

    /**
     * actionUrl을 기반으로 Action 정보를 조회합니다.
     * 현재 core-api 코드만으로는 actionUrl -> actionId 매핑 로직이 명확하지 않으므로,
     * 임시 actionId를 사용합니다. 실제 구현 시 이 부분을 보완해야 합니다.
     *
     * @param actionUrl 조회할 Action의 URL
     * @return Mono<Action> 조회된 Action 객체
     */
    public Mono<Action> findActionByUrl(String actionUrl) {
        // TODO: actionUrl을 actionId로 변환하는 로직이 필요합니다.
        // 현재 core-api 코드만으로는 actionUrl -> actionId 매핑 로직이 명확하지 않습니다.
        // 예시: Redis에 action_url:action_id 매핑 정보가 저장되어 있다고 가정
        Long dummyActionId = 1L; // 임시 actionId
        return cachedActionService.getActionById(dummyActionId);
    }

    /**
     * Action과 그것이 속한 ActionGroup의 활성화 상태를 모두 고려하여,
     * 해당 Action의 대기열 기능이 "실질적으로" 활성화되어 있는지 판단합니다.
     *
     * @param action 검사할 Action 객체
     * @return Mono<Boolean> 실질적인 활성화 여부
     */
    public Mono<Boolean> isActionEffectivelyEnabled(Action action) {
        if (action == null || action.getActionType() == null) {
            return Mono.just(false);
        }
        // ActionGroup 정보를 CachedActionService를 통해 가져옵니다.
        return cachedActionService.getActionGroupById(action.getActionGroupId())
            .map(ActionGroup::getEnabled)
            .defaultIfEmpty(false);
    }
}
