package com.winten.greenlight.prototype.core.api.controller.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventCacheResponseDto {
    private String eventId;
    private String result;
}