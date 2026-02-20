package com.winten.greenlight.core.api.controller.v2.ticket;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private String ticketId;
    private String roomId;
    private WaitStatus waitStatus;
    private String hash;
}