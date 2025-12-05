package com.winten.greenlight.core.support.util;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisKeyBuilder {
    @Value("${redis.key-prefix}")
    private String prefix;

    // TODO 한눈에 보기 쉽게 완성된 full string을 주석에 추가하기
    public String actionGroupMeta(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":meta";
    }

    public String actionGroupStatus(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":status";
    }

    public String action(Long actionId) {
        return prefix + ":action:" + actionId;
    }

    public String queue(Long actionGroupId, WaitStatus waitStatus) {
        return prefix + ":action_group:" + actionGroupId + ":queue:" + waitStatus;
    }

    // 대기열 키 (기존 queue 메서드와 유사하지만, actionId를 직접 받도록)
    // 기존 queue(Long actionGroupId, WaitStatus waitStatus)와는 다름
    public String waitingQueue(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":queue:" + WaitStatus.WAITING;
    }

    public String userApiKey() {
        return prefix + ":admin:user_api_key";
    }

    public String allActions() {
        return prefix + ":action:*";
    }

    public String actionEventStream() {
        return prefix + ":infra:action_event:stream";
    }

    public String landingCacheKey(String landingId) {
        return prefix + ":landing_action_mapping:" + landingId;
    }

    public String actionGroupRequestLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":request_log";
    }

    public String actionGroupAccessLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":access_log";
    }

    // TODO 활성사용자 수 체크인데, session으로 이름지어져있음. 추후 key 명칭 변경 필요
    public String actionGroupSession() {
        return prefix + ":session";
    }

    public String customerSession(String customerId) {
        return prefix + ":customer:session:" + customerId;
    }

    public String actionVersion() {
        return prefix + ":api:action:version";
    }
}