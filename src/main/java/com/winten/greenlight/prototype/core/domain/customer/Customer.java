package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.Data;

@Data
public class Customer {
    private String customerId;
    private double score;
    private Event event;
}