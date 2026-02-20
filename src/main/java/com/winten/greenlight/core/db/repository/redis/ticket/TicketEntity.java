package com.winten.greenlight.core.db.repository.redis.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketEntity {
    private String roomId;
    private String ticketId;
    private Long timestamp;
    private String hash;
    private int remainingUses;
}