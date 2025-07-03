package com.winten.greenlight.prototype.core.api.controller.action;

import com.winten.greenlight.prototype.core.domain.action.Action;
import com.winten.greenlight.prototype.core.domain.action.ActionRule;
import com.winten.greenlight.prototype.core.domain.action.ActionType;
import com.winten.greenlight.prototype.core.domain.action.DefaultRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionResponse {
    private Long id;
    private Long actionGroupId;
    private String ownerId;
    private String name;
    private String actionUrl;
    private ActionType actionType;
    private String landingId;
    private LocalDateTime landingStartAt;
    private LocalDateTime landingEndAt;
    private String landingDestinationUrl;
    private DefaultRuleType defaultRuleType;
    private List<ActionRule> actionRules;

    public static ActionResponse from(final Action action) {
        return ActionResponse.builder()
                .id(action.getId())
                .actionGroupId(action.getActionGroupId())
                .ownerId(action.getOwnerId())
                .name(action.getName())
                .actionUrl(action.getActionUrl())
                .actionType(action.getActionType())
                .landingId(action.getLandingId())
                .landingStartAt(action.getLandingStartAt())
                .landingEndAt(action.getLandingEndAt())
                .landingDestinationUrl(action.getLandingDestinationUrl())
                .defaultRuleType(action.getDefaultRuleType())
                .actionRules(action.getActionRules())
                .build();
    }
}