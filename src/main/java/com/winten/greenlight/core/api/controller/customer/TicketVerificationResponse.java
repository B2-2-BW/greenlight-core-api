package com.winten.greenlight.core.api.controller.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.domain.customer.Customer;
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

    public static TicketVerificationResponse success(Customer customer) {
        return TicketVerificationResponse.builder()
                .actionId(customer.getActionId())
                .actionGroupId(customer.getActionGroupId())
                .customerId(customer.getCustomerId())
                .verified(true)
                .build();
    }
    public static TicketVerificationResponse fail(Customer customer) {
        return TicketVerificationResponse.fail(customer, null);
    }

    public static TicketVerificationResponse fail(Customer customer, String reason) {
        return TicketVerificationResponse.builder()
                .actionId(customer.getActionId())
                .actionGroupId(customer.getActionGroupId())
                .customerId(customer.getCustomerId())
                .verified(false)
                .reason(reason)
                .build();
    }
}