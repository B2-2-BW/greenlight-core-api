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

    public String accessLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":accesslog";
    }
    public String queue(Long actionGroupId, WaitStatus waitStatus) {
        return prefix + ":action_group:" + actionGroupId + ":queue:" + waitStatus;
    }
    // 활성 사용자 수를 저장하는 ZSET의 키
    public String activeUsers(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":active_users";
    }
    // 토큰 메타데이터를 저장하는 Hash의 키
    public String token(String jwt) {
        return prefix + ":token:" + jwt;
    }

    // customerId와 actionId로 JWT를 찾는 인덱스 키 (String)
    public String customerActionTokenIndex(String customerId, Long actionId) {
        return String.format("%s:customer:%s:action:%d:jwt", prefix, customerId, actionId);
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
}