package com.winten.greenlight.core.domain.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long position;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long behindCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long queueSize;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long estimatedWaitTime;
}