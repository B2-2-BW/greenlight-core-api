package com.winten.greenlight.core.domain.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {
    private String roomId;
    private String adImageUrl;
    private String ticketId;
    private Long timestamp;
    private String hash;
    private WaitStatus waitStatus;
    private int remainingUses;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long waitTimeMs;

    public static Ticket bypassed(String roomId, String ticketId) {
        return Ticket.builder()
                .roomId(roomId)
                .ticketId(ticketId)
                .waitStatus(WaitStatus.BYPASSED)
                .build();
    }
}