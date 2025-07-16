package com.winten.greenlight.prototype.core.api.controller.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.prototype.core.domain.customer.Customer;
import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import com.winten.greenlight.prototype.core.support.error.CoreException;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TicketVerificationResponse {
    private Long actionId;
    private String customerId;
    private Boolean verified;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String detail;

    public static TicketVerificationResponse of(Customer customer) {
        var res = new TicketVerificationResponse();
        res.setActionId(customer.getActionId());
        res.setCustomerId(customer.getCustomerId());
        res.setVerified(customer.getWaitStatus() == WaitStatus.ENTERED);
        return res;
    }

    public static TicketVerificationResponse of(Throwable e) {
        var res = new TicketVerificationResponse();
        res.setVerified(false);

        if (e instanceof CoreException) {
            res.setDetail(((CoreException) e).getDetail().toString());
        } else {
            res.setDetail(e.getMessage());
        }

        return res;
    }
}