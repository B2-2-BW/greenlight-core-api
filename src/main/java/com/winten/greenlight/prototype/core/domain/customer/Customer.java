package com.winten.greenlight.prototype.core.domain.customer;

import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private String customerId;
    private double score;
    private WaitingPhase waitingPhase;

    public static Customer waiting(String customerId) {
        return new Customer(customerId, 0.0, WaitingPhase.WAITING);
    }
    public static Customer ready(String customerId) {
        return new Customer(customerId, 0.0, WaitingPhase.READY);
    }

    public Event toEvent() {
        if (customerId == null || !customerId.contains(":")) {
            return new Event();
        }
        String eventName = customerId.split(":", 2)[0];
        return Event.name(eventName);
    }
}