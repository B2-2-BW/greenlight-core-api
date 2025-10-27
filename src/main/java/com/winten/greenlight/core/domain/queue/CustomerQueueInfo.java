package com.winten.greenlight.core.domain.queue;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerQueueInfo {
    /**
     * 고객의 고유 식별자입니다.
     */
    private String customerId;

    /**
     * 대기열에서의 현재 위치(순번)입니다. (1부터 시작)
     * 이 값이 0이면 대기자가 없거나 입장이 가능한 상태를 의미할 수 있습니다.
     */
    private Long position;

    /**
     * 내 앞에 있는 사람 수(= position - 1)
     */
    private Long aheadCount;

    /*
     * 내 뒤에 있는 사람 수
     */
    private Long behindCount;

    /**
     * 현재 대기열에 있는 전체 인원 수입니다.
     */
    private Long queueSize;

    /**
     * 예상 대기 시간(초 단위)입니다.
     * 이 값은 추정치이며 실시간으로 변동될 수 있습니다.
     */
    private Long estimatedWaitTime;

    /**
     * 고객의 현재 대기/입장 상태입니다.
     * (예: WAITING, ALLOWED, ENTERED)
     */
    private WaitStatus waitStatus;
}