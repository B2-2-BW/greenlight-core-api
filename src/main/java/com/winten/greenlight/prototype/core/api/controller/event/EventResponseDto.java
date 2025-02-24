package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.Data;

@Data
public class EventResponseDto {
    private String eventId;
    private String eventName;
    private String eventUrl;

    public EventResponseDto(Event event) {
        this.eventId = event.getEventId();
        this.eventName = event.getEventName();
        this.eventUrl = event.getEventUrl();
    }
}