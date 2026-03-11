package com.winten.greenlight.core.db.repository.redis.room;

import lombok.Getter;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Getter
public class RoomRedisScript {
    public final RedisScript<Long> increaseMetricCountRedisScript = RedisScript.of("""
        local ttlSeconds = tonumber(ARGV[1])
        
        local count = redis.call('INCR', KEYS[1])
        if count == 1 then
            redis.call('EXPIRE', KEYS[1], ttlSeconds)
        end
        return count
        """, Long.class);
}