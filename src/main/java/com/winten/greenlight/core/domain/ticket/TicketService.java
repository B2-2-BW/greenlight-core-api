package com.winten.greenlight.core.domain.ticket;

import com.github.f4b6a3.ulid.Ulid;
import com.winten.greenlight.core.api.controller.v2.ticket.TicketIssueRequest;
import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketRepository;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.room.RoomService;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import com.winten.greenlight.core.support.publisher.RoomEventPublisher;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {
    private final RedisKeyBuilder redisKeyBuilder;
    private final QueueRepository queueRepository;
    private final RoomEventPublisher roomEventPublisher;
    private final TicketRepository ticketRepository;
    private final RoomService roomService;
    private final RoomRepository roomRepository;

    public Mono<Ticket> issueWaitingTicket(TicketIssueRequest request, String apiKey) {
        return roomService.findRoomById(request.getRoomId())
                .flatMap(room -> {
                    if (!room.getEnabled()) {
                        return Mono.empty(); // TODO actionGroup 비활성화 되어있을 때 바로 입장 가능하도록 반환하기
                    }
                    // TODO room apikey 검증하기
                    var ticketId = request.getTicketId() != null ? request.getTicketId() : generateNewTicketId();
                    return processWaitingTicket(room, ticketId);
                });
    }

    private String generateNewTicketId() {
        return Ulid.fast().toLowerCase();
    }


    private Mono<Ticket> processWaitingTicket(Room room, String ticketId) {

        Mono<Boolean> waitingRequired = this.isWaitingRequired(room.getRoomId(), room.getCapacity());

        return waitingRequired.flatMap(isWaitingRequired -> {
            WaitStatus status = isWaitingRequired ? WaitStatus.WAITING : WaitStatus.READY;
            var now = System.currentTimeMillis();

            var ticket = Ticket.builder()
                    .roomId(room.getRoomId())
                    .ticketId(ticketId)
                    .remainingUses(1)
                    .timestamp(now)
                    .waitStatus(status)
                    .build();

            Mono<Void> saveTicket = this.saveTicket(ticket)
                    .flatMap(saved -> {
                        if (status == WaitStatus.WAITING) {
                            String roomKey = redisKeyBuilder.roomQueue(room.getRoomId(), status);
                            return roomRepository.add(roomKey, ticketId, System.currentTimeMillis())
                                    .then();
                        }
                        return Mono.empty();
                    });

            // 비동기 로깅 작업
            Mono.when(
                    roomEventPublisher.publish(ticket) // influxDB에 현재 이벤트 기록 (대기, 입장준비 등)
//                    actionRepository.putRequestLog(actionGroup.getId(), customerId), // 활성사용자수 계산을 위한 접속기록 로깅
//                    actionRepository.putSession(CustomerUtil.parseKeyFromCustomerId(customerId)) // 5분 동시접속자 수 계산을 위한 로깅
            ).subscribeOn(Schedulers.boundedElastic()) // 별도 스레드에서 실행
            .subscribe(
                    null,
                    e -> log.error("[Async Log Error] failed for ticket: {}", ticketId, e) // 에러 발생 시 로그 남김
            );

            return saveTicket.thenReturn(ticket);
        });
    }

    private Mono<Boolean> saveTicket(Ticket ticket) {
        var ttl = Duration.ofMinutes(999);
        return ticketRepository.saveTicket(ticket, ttl);
    }

    private Mono<Boolean> isWaitingRequired(String roomId, int roomCapacity) {
        return roomRepository.countCustomersInRoomQueue(roomId, WaitStatus.WAITING)
                .flatMap(waiting -> {
                    if (waiting > 0) {
                        return Mono.just(true);
                    }
                    return roomRepository.countCustomersInRoomQueue(roomId, WaitStatus.ENTERED)
                                    .map(enterCount -> (roomCapacity - enterCount) < 1);
                });
    }

    public Mono<TicketVerification> verifyTicket(String ticketId) {
        return ticketRepository.findTicketById(ticketId)
            .flatMap(ticket -> {
                if (ticket.getWaitStatus() == WaitStatus.ENTERED) { // 이미 대기열에 한번 입장했던 고객인 경우 바로 입장 TODO 이후 remainingCount 등을 봐야할듯?
                    return Mono.just(TicketVerification.success(ticketId, ticket.getRoomId()));
                }
                if (ticket.getWaitStatus() != WaitStatus.READY) { // 예외케이스, 대기가 완료되지 않은 경우
                    return Mono.just(TicketVerification.fail(ticketId, "대기 ID가 유효하지 않거나 대기가 완료되지 않았습니다.")); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                }

                // TODO 대기가 정상적으로 완료됐다면 (ticket status가 READY 라면) 진행~~

                var now = System.currentTimeMillis();
                ticket.setTimestamp(now);
                ticket.setWaitTimeMs(now - ticket.getTimestamp()); // 고객의 score와 지금 시간차만큼 대기한 것으로 판단
                ticket.setWaitStatus(WaitStatus.ENTERED);

                return this.saveTicket(ticket)
                    .flatMap(updated -> roomRepository.findRoomById(ticket.getRoomId())
                                .defaultIfEmpty(Room.builder().roomId("unknown").build())
                                .flatMap(room -> {
                                    if ("unknown".equals(room.getRoomId())) {
                                        log.error("room not found roomId: {}, ticketId: {}", ticket.getRoomId(), ticketId);
                                    }
                                    String roomKey = redisKeyBuilder.roomQueue(room.getRoomId(), WaitStatus.ENTERED);
                                    return queueRepository.add(roomKey, ticketId, System.currentTimeMillis());
                                })
                    )
                    .map(enteredResult -> TicketVerification.success(ticketId, ticket.getRoomId()))
                    .doOnNext(verification -> {
                        Mono.when(
                                        roomEventPublisher.publish(ticket)
                            // actionRepository.putAccessLog(ticket.getActionGroupId(), ticket.getCustomerId()),
                            // actionRepository.putSession(CustomerUtil.parseKeyFromCustomerId(ticketId))
                                ).subscribeOn(Schedulers.boundedElastic()) // 별도 스레드에서 실행
                                .subscribe(
                                        null,
                                        e -> log.error("[Async Log Error] failed for ticket: {}", ticketId, e) // 에러 발생 시 로그 남김
                                );
                    })
                    // 업데이트 실패 등으로 위 체인이 빈 값(Mono.empty)이 되면 여기로 옴
                    .defaultIfEmpty(TicketVerification.fail(ticketId, "입장 처리에 실패했습니다. (상태 변경 불가 등)"));
            })
            .onErrorResume(e -> {
                log.error("verifyTicket system error", e); // 시스템 에러 로그 추가 권장
                if (e instanceof CoreException) {
                    return Mono.error(e);
                } else {
                    return Mono.error(CoreException.of(ErrorCode.INTERNAL_SERVER_ERROR, "입장에 실패하였습니다 " + e));
                }
            });
    }

    public Mono<TicketStatus> getTicketStatus(String ticketId) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(this::buildTicketStatus)
                .switchIfEmpty(Mono.error(new CoreException(ErrorCode.TICKET_NOT_FOUND, "존재하지 않는 Ticket ID입니다: " + ticketId)));
    }

    private Mono<TicketStatus> buildTicketStatus(Ticket ticket) {
        var roomId = ticket.getRoomId();
        var ticketId = ticket.getTicketId();
        var waitStatus = ticket.getWaitStatus();

        if (waitStatus == WaitStatus.READY || waitStatus == WaitStatus.ENTERED) {
            var status = TicketStatus.builder().ticketId(ticketId).waitStatus(waitStatus).build();
            return Mono.just(status);
        }
        return Mono.zip(ticketRepository.findWaitingPosition(roomId, ticketId),
                roomRepository.countCustomersInRoomQueue(roomId, WaitStatus.WAITING)
        ).handle((tuple, sink) -> {
            long rank = tuple.getT1(); // Redis Rank (0-based index)
            long totalCount = tuple.getT2();
            if (rank == -1L) {
                sink.error(new CoreException(ErrorCode.INVALID_TICKET_STATE, "대기열에서 티켓을 찾을 수 없습니다."));
                return;
            }

            long myPosition = rank + 1; // 1등이 1번
            long behindCount = Math.max(totalCount - myPosition, 0); // 내 뒤에 있는 사람 수
            // TODO: 예상 대기 시간 계산 로직 수정 (예: 1명당 30초 * 앞사람 수)
            long estimatedWaitTime = 0L;
            sink.next(TicketStatus.builder()
                    .ticketId(ticketId)
                    .waitStatus(waitStatus)
                    .position(myPosition)
                    .behindCount(behindCount)
                    .estimatedWaitTime(estimatedWaitTime)
                    .build()
            );
        });
    }
}