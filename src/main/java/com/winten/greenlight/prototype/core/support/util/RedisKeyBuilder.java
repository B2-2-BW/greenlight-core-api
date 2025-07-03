package com.winten.greenlight.prototype.core.support.util;

import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisKeyBuilder {
    @Value("${redis.key-prefix}")
    private String prefix;

    public String actionGroupMeta(Long actionGroupId) {
        return String.format("%s:action_group:%d:meta", prefix, actionGroupId);
    }

    public String actionGroupStatus(Long actionGroupId) {
        return String.format("%s:action_group:%d:status", prefix, actionGroupId);
    }

    public String action(Long actionGroupId, Long actionId) {
        return String.format("%s:action_group:%d:action:%d", prefix, actionGroupId, actionId);
    }

    public String accessLog(Long actionGroupId) {
        return String.format("%s:action_group:%d:accesslog", prefix, actionGroupId);
    }
    public String queue(Long actionGroupId, WaitStatus waitStatus) {
        return String.format("%s:action_group:%d:queue:%s", prefix, actionGroupId, waitStatus);
    }
}