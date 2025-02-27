package com.winten.greenlight.prototype.core.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerQueueInfo {
    private String customerId;
    private Long position;
    private Long queueSize;
    private Long estimatedWaitTime;
    private WaitingPhase waitingPhase;
}
