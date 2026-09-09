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

    /**
     * KEYS: waitingQueue, enteredQueue, waitingHeartbeat, enteredHeartbeat, waitingMetric, enteredMetric
     * ARGV: ticketId, queueScore, heartbeatScore, capacity, metricTtlSeconds
     * return: 0 = WAITING, 1 = ENTERED
     */
    public final RedisScript<Long> issueTicketRedisScript = RedisScript.of("""
        local ticketId = ARGV[1]
        local queueScore = tonumber(ARGV[2])
        local heartbeatScore = tonumber(ARGV[3])
        local capacity = tonumber(ARGV[4])
        local ttlSeconds = tonumber(ARGV[5])

        local waiting = redis.call('ZCARD', KEYS[1])
        local status = 0
        if waiting == 0 then
            local entered = redis.call('ZCARD', KEYS[4])
            if (capacity - entered) >= 1 then
                status = 1
            end
        end

        if status == 1 then
            redis.call('ZADD', KEYS[2], queueScore, ticketId)
            redis.call('ZADD', KEYS[4], heartbeatScore, ticketId)
        else
            redis.call('ZADD', KEYS[1], queueScore, ticketId)
            redis.call('ZADD', KEYS[3], heartbeatScore, ticketId)
        end

        local waitingMetric = redis.call('INCR', KEYS[5])
        if waitingMetric == 1 then
            redis.call('EXPIRE', KEYS[5], ttlSeconds)
        end
        if status == 1 then
            local enteredMetric = redis.call('INCR', KEYS[6])
            if enteredMetric == 1 then
                redis.call('EXPIRE', KEYS[6], ttlSeconds)
            end
        end

        return status
        """, Long.class);
}