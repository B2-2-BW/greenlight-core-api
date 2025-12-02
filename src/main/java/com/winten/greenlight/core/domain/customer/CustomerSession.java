package com.winten.greenlight.core.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSession {
    private Long actionGroupId;
    private Long actionId;
    private String customerId;
    private Long timestamp;
    private WaitStatus waitStatus;
    private boolean verified;
    private Long accessCount;
    private String destinationUrl;
    private Long waitTimeMs;

    public static CustomerSession bypassed() {
        return CustomerSession.builder()
                .timestamp(System.currentTimeMillis())
                .waitStatus(WaitStatus.BYPASSED)
                .build();
    }

    public String uniqueId() {
        try {
            return customerId.split(":")[1];
        } catch (Exception e) {
            return customerId;
        }
    }
}