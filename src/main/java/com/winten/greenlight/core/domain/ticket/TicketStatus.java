package com.winten.greenlight.core.domain.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String greenlightToken;
    private Long position;
    private Long behindCount;
    private Long queueSize;
    private Long estimatedWaitTime; // 예상 대기시간(초). 예측할 수 없으면 -1
    @JsonIgnore
    private long retryAfter;
}
