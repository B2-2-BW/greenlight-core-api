package com.winten.greenlight.core.api.controller.queue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.Data;

@Data
public class CustomerSessionResponse {
    private Long actionId;
    private Long actionGroupId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String customerId;
    private String destinationUrl;
    private Long timestamp;
    private WaitStatus waitStatus;
}