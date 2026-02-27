package com.winten.greenlight.core.domain.ticket;

import com.winten.greenlight.core.api.controller.v2.ticket.TicketResponse;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketConverter {
    TicketResponse toResponse(Ticket ticket);
    TicketEntity toEntity(Ticket ticket);
}