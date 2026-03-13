package com.winten.greenlight.core.api.controller.v2.room;

import com.winten.greenlight.core.domain.room.RoomConverter;
import com.winten.greenlight.core.domain.room.RoomService;
import com.winten.greenlight.core.domain.ticket.TicketStatus;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v2/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomConverter roomConverter;

    @GetMapping("{roomId}")
    public Mono<ResponseEntity<RoomResponse>> getTicketStatus(
            @PathVariable String roomId
    ) {
        return roomService.findRoomById(roomId)
                .map(room -> ResponseEntity.ok()
                        .body(roomConverter.toResponse(room))
                );
    }
}