package com.winten.greenlight.core.domain.room;

import com.winten.greenlight.core.api.controller.v2.room.RoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomConverter {
    RoomResponse toResponse(Room room);
}