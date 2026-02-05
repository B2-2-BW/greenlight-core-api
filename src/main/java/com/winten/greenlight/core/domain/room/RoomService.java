package com.winten.greenlight.core.domain.room;

import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    public Mono<Room> findRoomById(final String roomId) {
        return roomRepository.findRoomById(roomId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorCode.ROOM_NOT_FOUND, "Room을 찾을 수 없습니다. roomId: " + roomId)));
    }
}