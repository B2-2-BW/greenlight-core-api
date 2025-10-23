package com.winten.greenlight.core.api.controller.customer;

import com.winten.greenlight.core.domain.customer.CustomerEntry;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerEntryRequest {
    private Long actionId;

    public CustomerEntry toCustomerEntry(long timestamp) {
        return CustomerEntry.builder()
                .actionId(actionId)
                .timestamp(timestamp)
                .build();
    }
}