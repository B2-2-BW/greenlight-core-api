package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.WaitingPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerQueueInfoResponseDto {
    private String customerId;
    private Long position;
    private Long queueSize;
    private Long estimatedWaitTime;
    private WaitingPhase waitingPhase;
}
