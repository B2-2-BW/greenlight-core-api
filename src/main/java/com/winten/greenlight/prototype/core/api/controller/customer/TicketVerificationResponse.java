package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerEntry;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TicketVerificationResponse {
    private Long actionId;
    private String customerId;
    private Boolean verified;

    public static TicketVerificationResponse of(CustomerEntry entry) {
        var res = new TicketVerificationResponse();
        res.setActionId(entry.getActionId());
        res.setCustomerId(entry.getCustomerId());
        return res;
    }
}