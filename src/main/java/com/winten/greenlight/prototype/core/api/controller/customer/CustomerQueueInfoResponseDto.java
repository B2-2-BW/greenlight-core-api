package com.winten.greenlight.prototype.core.api.controller.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerQueueInfoResponseDto {
    private Long position;
    private Long queueSize;
    private Long estimatedWaitTime;
}