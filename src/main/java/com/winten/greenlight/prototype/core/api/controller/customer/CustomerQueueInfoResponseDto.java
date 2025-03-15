package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.domain.customer.CustomerQueueInfo;
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

    public CustomerQueueInfoResponseDto(CustomerQueueInfo customerQueueInfo) {
        this.customerId = customerQueueInfo.getCustomerId();
        this.position = customerQueueInfo.getPosition();
        this.queueSize = customerQueueInfo.getQueueSize();
        this.estimatedWaitTime = customerQueueInfo.getEstimatedWaitTime();
        this.waitingPhase = customerQueueInfo.getWaitingPhase();
    }
}