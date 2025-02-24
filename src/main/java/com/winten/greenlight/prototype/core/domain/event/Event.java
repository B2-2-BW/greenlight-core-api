package com.winten.greenlight.prototype.core.domain.event;

import com.winten.greenlight.prototype.core.db.repository.customer.EventStringEntity;
import lombok.Data;

import java.io.Serializable;

@Data
public class Event implements Serializable {
    private String eventId;
    private String eventName;
    private String eventUrl;

    public Event(final String eventId, final String eventName, final String eventUrl) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventUrl = eventUrl;
    }

    public Event(final EventStringEntity entity) {
        this.eventId = entity.getEventId();
        this.eventName = entity.getEventName();
        this.eventUrl = entity.getEventUrl();
    }
}