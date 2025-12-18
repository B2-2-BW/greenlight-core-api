package com.winten.greenlight.core.domain.action;

import com.winten.greenlight.core.db.repository.redis.action.ActionRepository;
import com.winten.greenlight.core.domain.queue.ActionConfig;
import com.winten.greenlight.core.domain.queue.SystemStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionService {
    private final ActionRepository actionRepository;

    public Mono<List<Action>> getAllActions() { // TODO 본인 사이트만 조회 가능하도록 수정
        return actionRepository.getAllActions();
    }

    public Mono<List<Action>> getAllEnabledActions() { // TODO 본인 사이트만 조회 가능하도록 수정
        return actionRepository.getAllEnabledActions();
    }

    public Mono<Action> getActionById(final Long actionId) {
        return actionRepository.getActionById(actionId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.ACTION_NOT_FOUND, "Action을 찾을 수 없습니다. actionId: " + actionId)));
    }

    public Mono<Action> getActionByLandingId(String landingId) {
        return actionRepository.getActionIdByLandingId(landingId)
                .flatMap(actionId -> getActionById(Long.valueOf(actionId)));
    }

    public Mono<ActionGroup> getActionGroupById(final Long actionGroupId) {
        return actionRepository.getActionGroupById(actionGroupId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorType.ACTION_GROUP_NOT_FOUND, "Action Group을 찾을 수 없습니다. actionGroupId: " + actionGroupId)));
    }

    public Mono<ActionConfig> getActionConfig(String version) {
        return actionRepository.getCurrentActionVersion()
                .flatMap(currentVersion -> {
                    if (currentVersion != null && currentVersion.equals(version)) {
                        return Mono.error(CoreException.of(ErrorType.NOT_MODIFIED));
                    }
                    return getAllEnabledActions()
                            .map(actions -> ActionConfig.builder()
                                    .actions(actions)
                                    .version(currentVersion)
                                    .systemStatus(SystemStatus.RUNNING) // TODO SystemStatus.ON 시스템 상태 반환기능 없음. 구현필요
                                    .build()
                            );
                });
    }

}