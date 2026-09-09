package com.winten.greenlight.core.domain.room;

import com.winten.greenlight.core.domain.site.SiteOperationStatus;
import com.winten.greenlight.core.support.cache.MetaLocalCache;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final MetaLocalCache metaLocalCache;

    public Mono<Room> findRoomById(final String roomId) {
        return metaLocalCache.getRoom(roomId)
                .switchIfEmpty(Mono.error(CoreException.of(ErrorCode.ROOM_NOT_FOUND, "Room을 찾을 수 없습니다. roomId: " + roomId)));
    }

    public Mono<SiteOperationStatus> findSiteOperationStatus(final String siteId) {
        return metaLocalCache.getSiteOperationStatus(siteId);
    }
}