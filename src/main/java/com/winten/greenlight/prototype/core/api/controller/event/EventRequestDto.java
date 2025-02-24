package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.Data;

@Data
public class EventRequestDto {
    private String eventId;

    public Event toEvent() {
        return new Event(eventId, null, null);
    }
}