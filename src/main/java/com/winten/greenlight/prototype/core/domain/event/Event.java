package com.winten.greenlight.prototype.core.domain.event;

import com.winten.greenlight.prototype.core.db.repository.redis.event.EventEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event implements Serializable {
    private String eventName;
    private String eventDescription;
    private String eventType;
    private String eventUrl;
    private Integer queueBackpressure;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;

    public Event(final EventEntity entity) {
        this.eventName = entity.getEventName();
        this.eventDescription = entity.getEventDescription();
        this.eventType = entity.getEventType();
        this.eventUrl = entity.getEventUrl();
        this.queueBackpressure = entity.getQueueBackpressure();
        this.eventStartTime = entity.getEventStartTime();
        this.eventEndTime = entity.getEventEndTime();
    }

    public static Event name(final String name) {
        return Event.builder().eventName(name).build();
    }
}