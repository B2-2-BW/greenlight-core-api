package com.winten.greenlight.core.api.controller.action;

import com.winten.greenlight.core.domain.action.ActionService;
import com.winten.greenlight.core.domain.action.CachedActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Action, ActionGroup 조회 및 캐시 초기화 컨트롤러. 기능이 많지 않아서 하나의 컨트롤러로 합쳤음
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ActionController {
    private final CachedActionService cachedActionService;
    private final ActionService actionDomainService;

    @GetMapping("/action-groups")
    public Mono<ResponseEntity<ActionGroupResponse>> getActionGroupByActionId(@RequestParam final Long actionId) {
        return cachedActionService.getActionById(actionId)
                .flatMap(action -> cachedActionService.getActionGroupById(action.getActionGroupId()))
                .flatMap(actionGroup -> Mono.just(ResponseEntity.ok(ActionGroupResponse.from(actionGroup))));
    }

    @GetMapping("/action-groups/{actionGroupId}")
    public Mono<ResponseEntity<ActionGroupResponse>> getActionGroupById(@PathVariable final Long actionGroupId) {
        return cachedActionService.getActionGroupById(actionGroupId)
                .flatMap(action -> Mono.just(ResponseEntity.ok(ActionGroupResponse.from(action))));
    }

    @GetMapping("/actions/{actionId}")
    public Mono<ResponseEntity<ActionResponse>> getActionById(@PathVariable final Long actionId) {
        return cachedActionService.getActionById(actionId)
                .flatMap(action -> Mono.just(ResponseEntity.ok(ActionResponse.from(action))));
    }

    @GetMapping("/actions/landing/{landingId}")
    public Mono<ResponseEntity<ActionResponse>> getActionByLandingId(@PathVariable final String landingId) {
        return cachedActionService.getActionByLandingId(landingId)
                .flatMap(action -> Mono.just(ResponseEntity.ok(ActionResponse.from(action))));
    }

    @GetMapping("/actions")
    public Flux<ActionResponse> getActionGroupByKey(
            @RequestHeader("X-GREENLIGHT-API-KEY") String greenlightApiKey
    ) {
        return actionDomainService.getActionsFromApiKey(greenlightApiKey)
                .map(ActionResponse::from);
    }

}