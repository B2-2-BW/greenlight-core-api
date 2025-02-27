package com.winten.greenlight.prototype.core.api.controller.event;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponseDto {
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;

    public EventResponseDto(Event event) {
        this.eventName = event.getEventName();
        this.eventDescription = event.getEventDescription();
        this.eventType = event.getEventType();
        this.eventUrl = event.getEventUrl();
        this.queueBackpressure = event.getQueueBackpressure();
        this.eventStartTime = event.getEventStartTime();
        this.eventEndTime = event.getEventEndTime();
    }
}