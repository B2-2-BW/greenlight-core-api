package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.Data;

@Data
public class EventRequestDto {
    private String eventName;

    public Event toEvent() {
        Event event = new Event();
        event.setEventName(eventName);
        return event;
    }
}