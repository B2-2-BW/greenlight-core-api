package com.winten.greenlight.core.api.controller.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.domain.customer.CustomerSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketVerificationResponse {
    private Long actionId;
    private Long actionGroupId;
    private String customerId;
    private Boolean verified;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String reason;

    public static TicketVerificationResponse success(CustomerSession customerSession) {
        return TicketVerificationResponse.builder()
                .actionId(customerSession.getActionId())
                .actionGroupId(customerSession.getActionGroupId())
                .customerId(customerSession.getCustomerId())
                .verified(true)
                .build();
    }

    public static TicketVerificationResponse fail(String customerId, String reason) {
        return TicketVerificationResponse.builder()
                .customerId(customerId)
                .verified(false)
                .reason(reason)
                .build();
    }
}