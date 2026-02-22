package com.winten.greenlight.core.api.controller.v2.ticket;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketHeartbeatRequest {
    private WaitStatus heartbeatType;
}
