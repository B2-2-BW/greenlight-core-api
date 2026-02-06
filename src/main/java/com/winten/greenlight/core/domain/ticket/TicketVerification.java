package com.winten.greenlight.core.domain.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketVerification {
    private String ticketId;
    private String roomId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String reason;

    public static TicketVerification success(String ticketId, String roomId) {
        return TicketVerification.builder()
                .ticketId(ticketId)
                .roomId(roomId)
                .build();
    }

    public static TicketVerification fail(String ticketId, String reason) {
        return TicketVerification.builder()
                .ticketId(ticketId)
                .reason(reason)
                .build();
    }
}