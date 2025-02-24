package com.winten.greenlight.prototype.core.db.repository.customer;

import lombok.Data;

import java.io.Serializable;

@Data
public class EventStringEntity implements Serializable {
    private String eventId;
    private String eventName;
    private String eventUrl;
}