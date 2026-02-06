package com.winten.greenlight.core.domain.ticket;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketConverter {
    TicketStatus toStatus(Ticket ticket);
}