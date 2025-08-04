package com.winten.greenlight.prototype.core.domain.action;

import com.winten.greenlight.prototype.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.prototype.core.db.repository.redis.action.ActionRuleRepository;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import com.winten.greenlight.prototype.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Action 도메인의 비즈니스 로직을 담당하는 서비스입니다.
 * Action 및 ActionGroup 정보 조회, 활성화 여부 판단 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class ActionService {
    private final ActionRepository actionRepository;

    public Flux<Action> getActionsFromApiKey(String greenlightApiKey) {
        return actionRepository.getUserApiKey()
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.REDIS_ERROR,"there is no cache userApiKey")))
                .flatMapMany(value -> {
                    if (!greenlightApiKey.equals(value)) {
                        return Flux.error(CoreException.of(ErrorType.BAD_REQUEST, "Invalid Api Key"));
                    }
                    return actionRepository.getAllActions();
                });
    }

}