package com.winten.greenlight.core.domain.ticket;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatus {
    private String ticketId;
    private String roomId;
    private WaitStatus waitStatus;
    private Long position;
    private Long behindCount;
    private Long queueSize;
    private Long estimatedWaitTime;
}