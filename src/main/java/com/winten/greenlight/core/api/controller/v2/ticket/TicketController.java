package com.winten.greenlight.core.api.controller.v2.ticket;

import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.ticket.TicketConverter;
import com.winten.greenlight.core.domain.ticket.TicketService;
import com.winten.greenlight.core.domain.ticket.TicketStatus;
import com.winten.greenlight.core.domain.ticket.TicketVerification;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v2/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    private final TicketConverter ticketConverter;

    /**
     * 1. 입장 요청 (대기열 진입)
     * <p>
     * 사용자가 대기열에 진입을 요청합니다.
     * Room Capacity가 남아있으면 바로 활성 상태(Active)를 반환하고,
     * 그렇지 않으면 대기표(Waiting Ticket)를 발급합니다.
     * </p>
     *
     * @return 생성된 티켓 정보 (대기 순번, 토큰 등)
     */
    @PostMapping("")
    public Mono<TicketResponse> issueTicket(
            @RequestBody final TicketIssueRequest request,
            @RequestHeader(name = "X-API-KEY", required = false) String apiKey
    ) {
        // TODO: 사용자 인증 정보 확인
        // TODO: 대기열 큐(Redis 등)에 사용자 등록 또는 바로 입장 처리
        // TODO: 발급된 티켓 정보(UUID, 순번, 예상시간 등) 반환
        return ticketService.issueWaitingTicket(request, apiKey)
                .map(ticketConverter::toResponse);
    }

    /**
     * 주기적으로 heartbeat를 호출해서 Ticket Session을 갱신
     */
    @PostMapping("{ticketId}/heartbeat")
    public Mono<?> heartbeatTicket(
            @PathVariable String ticketId,
            @RequestParam WaitStatus heartbeatType
    ) {
        return ticketService.updateHeartbeatFromTicketId(ticketId, heartbeatType);
    }

    /**
     * heartbeat를 삭제 Ticket Session을 갱신
     */
    @PostMapping("{ticketId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> leaveRoom(
            @PathVariable String ticketId,
            @RequestParam WaitStatus heartbeatType
    ) {
        return ticketService.deleteTicket(ticketId, heartbeatType);
    }

    @GetMapping("{ticketId}/status")
    public Mono<ResponseEntity<TicketStatus>> getTicketStatus(
            @PathVariable String ticketId,
            @RequestHeader("X-GREENLIGHT-TOKEN") @Nullable String greenlightToken
    ) {
        // TODO greenlightToken은 현재 미사용
        return ticketService.getTicketStatus(ticketId, greenlightToken)
                .map(status -> ResponseEntity.ok()
                .header("Retry-After", String.valueOf(status.getRetryAfter()))
                .body(status));
    }

    /**
     * 3. 입장 토큰 검증
     * <p>
     * 대기화면에서 메인 서비스로 진입 후, 정당한 절차를 거쳐 입장했는지 검증합니다.
     * (예: 프론트엔드 라우팅 전 또는 핵심 API 호출 전 검증)
     * </p>
     *
     * @param ticketId 검증할 티켓의 고유 ID
     * @return 검증 결과 (유효함/유효하지 않음) 및 접근 권한 토큰
     */
    @PostMapping("{ticketId}/verification")
    public Mono<TicketVerification> verifyTicket(
            @PathVariable String ticketId,
            @RequestBody TicketVerificationRequest request
    ) {
        return ticketService.verifyTicket(ticketId, request.getHash());
    }

}