package com.winten.greenlight.prototype.core.api.controller.action;

import com.winten.greenlight.prototype.core.domain.action.*;
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
public class ActionGroupResponse {
    private Long id;
    private String ownerId;
    private String name;
    private String description;
    private Integer maxActiveCustomers;
    private Boolean enabled;

    public static ActionGroupResponse from(final ActionGroup actionGroup) {
        return ActionGroupResponse.builder()
                .id(actionGroup.getId())
                .ownerId(actionGroup.getOwnerId())
                .name(actionGroup.getName())
                .description(actionGroup.getDescription())
                .maxActiveCustomers(actionGroup.getMaxActiveCustomers())
                .enabled(actionGroup.getEnabled())
                .build();
    }
}