package com.winten.greenlight.core.api.controller.action;

import com.winten.greenlight.core.domain.action.ActionGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionGroupResponse {
    private Long id;
    private String ownerId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Boolean enabled;

    public static ActionGroupResponse from(final ActionGroup actionGroup) {
        return ActionGroupResponse.builder()
                .id(actionGroup.getId())
                .ownerId(actionGroup.getOwnerId())
                .name(actionGroup.getName())
                .description(actionGroup.getDescription())
                .maxTrafficPerSecond(actionGroup.getMaxTrafficPerSecond())
                .enabled(actionGroup.getEnabled())
                .build();
    }
}