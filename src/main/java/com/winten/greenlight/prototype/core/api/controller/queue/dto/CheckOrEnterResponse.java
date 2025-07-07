package com.winten.greenlight.prototype.core.api.controller.queue.dto;

import com.winten.greenlight.prototype.core.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 응답을 위한 DTO (Data Transfer Object) 클래스입니다.
 * 클라이언트에게 대기열 진입 결과 및 상태를 전달하는 데 사용됩니다。
 * 위치: com.winten.greenlight.prototype.core.api.controller.queue.dto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckOrEnterResponse {
    /**
     * 현재 대기열 상태를 나타내는 문자열입니다.
     * - "WAITING": 대기열에 진입하여 순서를 기다리는 중.
     * - "ALLOWED": 대기열을 통과하여 서비스 진입이 허용됨.
     * - "EXISTING": 이미 유효한 토큰을 가지고 있음 (재접속 시).
     * - "DISABLED": 해당 액션이 현재 비활성화되어 대기열 진입 불가.
     * - "ACTION_NOT_FOUND": 요청한 액션(actionUrl)을 시스템에서 찾을 수 없음.
     */
    private String status; // "WAITING", "ALLOWED", "DISABLED", "ACTION_NOT_FOUND"
    private String token;
    private Long rank; // 대기 중일 경우의 예상 순번
}
