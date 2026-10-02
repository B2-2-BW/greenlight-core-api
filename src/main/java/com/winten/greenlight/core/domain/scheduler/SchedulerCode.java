package com.winten.greenlight.core.domain.scheduler;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 스케줄러 종류. greenlight-scheduler의 SchedulerCode와 같은 이름이어야 한다.
 */
@Getter
@AllArgsConstructor
public enum SchedulerCode {
    WAITING_TO_READY, /* 고객 입장처리 스케쥴러 (v2) */
    METRIC, /* 화면 이탈량 계산 스케쥴러 (v2) */
    REMOVE_EXPIRED, /* 만료된 데이터 삭제 스케쥴러 (v2) */

    RELOCATION, /* 고객 이동 스케쥴러 */
    CAPACITY, /* 대기열 활성사용자수 계산 스케쥴러 */
    CLEANUP_SESSION, /* session 정리 스케쥴러 */
    REDIS_CLEANUP, /* Redis stream 정리 스케쥴러 */
    UNKNOWN /* 알 수 없음 */
    ;

    public static SchedulerCode from(String value) {
        return SchedulerCode.valueOf(value.toUpperCase());
    }

    public static SchedulerCode of(String source) {
        try {
            return SchedulerCode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public String toValue() {
        return this.name().toUpperCase();
    }
}
