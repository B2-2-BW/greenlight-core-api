package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.event.Event;
import lombok.Data;

@Data
public class CustomerRequestDto {
    private String customerId;
    private String eventName;

    public Customer toCustomer() {
        var customer = new Customer();
        customer.setCustomerId(customerId);
        return customer;
    }

    public Event toEvent() {
        var event = new Event();
        event.setEventName(eventName);
        return event;
    }
}