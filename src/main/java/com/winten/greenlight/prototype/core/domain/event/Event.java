package com.winten.greenlight.prototype.core.domain.event;

import com.winten.greenlight.prototype.core.db.repository.redis.event.EventEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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

    public Event(final EventEntity entity) {
        this.eventName = entity.getEventName();
        this.eventDescription = entity.getEventDescription();
        this.eventType = entity.getEventType();
        this.eventUrl = entity.getEventUrl();
        this.queueBackpressure = entity.getQueueBackpressure();
    }
}