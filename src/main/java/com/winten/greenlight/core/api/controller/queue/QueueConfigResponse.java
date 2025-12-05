package com.winten.greenlight.core.api.controller.queue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.domain.action.*;
import com.winten.greenlight.core.domain.queue.SystemStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QueueConfigResponse {
    private SystemStatus systemStatus;
    private String version;
    private List<QueueConfigActionResponse> actions;

    @Data
    public static class QueueConfigActionResponse {
        private Long id;
        private Long actionGroupId;
        private String name;
        private boolean enabled;
        private String actionUrl;
        private ActionType actionType;
        private String landingId;
        private LocalDateTime landingStartAt;
        private LocalDateTime landingEndAt;
        private String landingDestinationUrl;
        private DefaultRuleType defaultRuleType;
        private List<ActionRule> actionRules;
    }
}