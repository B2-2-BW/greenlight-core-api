package com.winten.greenlight.core.domain.queue;

import com.winten.greenlight.core.domain.action.Action;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class QueueConfig {
    private SystemStatus systemStatus;
    private String version;
    private List<Action> actions;
}