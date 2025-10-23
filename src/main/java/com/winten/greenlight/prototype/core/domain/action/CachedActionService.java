package com.winten.greenlight.prototype.core.domain.action;

import com.winten.greenlight.prototype.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CachedActionService {
    private final ActionRepository actionRepository;

    // 로컬캐시 구조 제거
    // TODO 성능 확인 후 개선 필요 시 EhCache 등 TTL 기능이 있는 캐시로 전환 검토
//    @Cacheable(cacheNames = "action", key = "#actionId")
    public Mono<Action> getActionById(final Long actionId) {
        return actionRepository.getActionById(actionId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.ACTION_NOT_FOUND, "Action을 찾을 수 없습니다. actionId: " + actionId)));
    }

//    @Cacheable(cacheNames = "actionGroup", key = "#actionGroupId")
    public Mono<ActionGroup> getActionGroupById(final Long actionGroupId) {
        return actionRepository.getActionGroupById(actionGroupId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "Action Group을 찾을 수 없습니다. actionGroupId: " + actionGroupId)));
    }

//    @Cacheable(cacheNames = "landingActionMapping", key = "#landingId")
    public Mono<Action> getActionByLandingId(String landingId) {
        return actionRepository.getActionIdByLandingId(landingId)
                .flatMap(actionId -> getActionById(Long.valueOf(actionId)));
    }
}