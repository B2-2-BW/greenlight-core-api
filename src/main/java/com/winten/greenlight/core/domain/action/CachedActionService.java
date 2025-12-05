package com.winten.greenlight.core.domain.action;

import com.winten.greenlight.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CachedActionService {
    private final ActionService actionService;

    @Cacheable(cacheNames = "actionList")
    public Mono<List<Action>> getAllActions() { // TODO 본인 사이트만 조회 가능하도록 수정
        return actionService.getAllActions().collectList();
    }

    @CacheEvict(cacheNames = "actionList")
    public Mono<Void> invalidateActionListCache() { // TODO 본인 사이트만 조회 가능하도록 수정
        return Mono.empty();
    }

    @Cacheable(cacheNames = "actionCache", key = "#actionId")
    public Mono<Action> getActionById(final Long actionId) {
        return actionService.getActionById(actionId);
    }

    @Cacheable(cacheNames = "landingCache", key = "#landingId")
    public Mono<Action> getActionByLandingId(String landingId) {
        return actionService.getActionByLandingId(landingId);
    }

    @Cacheable(cacheNames = "actionGroupCache", key = "#actionGroupId")
    public Mono<ActionGroup> getActionGroupById(final Long actionGroupId) {
        return actionService.getActionGroupById(actionGroupId);
    }

}