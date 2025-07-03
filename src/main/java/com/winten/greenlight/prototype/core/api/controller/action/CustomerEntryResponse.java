package com.winten.greenlight.prototype.core.api.controller.action;

import com.winten.greenlight.prototype.core.domain.customer.CustomerEntry;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerEntryResponse {
    private Long actionId;
    private String customerId;

    public static CustomerEntryResponse of(CustomerEntry entry) {
        var res = new CustomerEntryResponse();
        res.setActionId(entry.getActionId());
        res.setCustomerId(entry.getCustomerId());
        return res;
    }
}