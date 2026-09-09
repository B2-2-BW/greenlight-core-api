package com.winten.greenlight.core.support.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.site.SiteOperationStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class MetaLocalCache {
    private final AsyncLoadingCache<String, Optional<Room>> roomCache;
    private final AsyncLoadingCache<String, SiteOperationStatus> siteCache;

    public MetaLocalCache(RoomRepository roomRepository) {
        this.roomCache = Caffeine.newBuilder()
                .maximumSize(LocalCacheConfig.META_MAXIMUM_SIZE)
                .expireAfterWrite(LocalCacheConfig.META_EXPIRE_AFTER_WRITE)
                .refreshAfterWrite(LocalCacheConfig.META_REFRESH_AFTER_WRITE)
                .buildAsync((roomId, executor) -> roomRepository.findRoomById(roomId)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
        this.siteCache = Caffeine.newBuilder()
                .maximumSize(LocalCacheConfig.META_MAXIMUM_SIZE)
                .expireAfterWrite(LocalCacheConfig.META_EXPIRE_AFTER_WRITE)
                .refreshAfterWrite(LocalCacheConfig.META_REFRESH_AFTER_WRITE)
                .buildAsync((siteId, executor) -> roomRepository.findSiteOperationStatus(siteId).toFuture());
    }

    public Mono<Room> getRoom(String roomId) {
        return Mono.defer(() -> Mono.fromFuture(roomCache.get(roomId)))
                .flatMap(room -> room.map(Mono::just).orElseGet(Mono::empty));
    }

    public Mono<SiteOperationStatus> getSiteOperationStatus(String siteId) {
        return Mono.defer(() -> Mono.fromFuture(siteCache.get(siteId)));
    }
}
