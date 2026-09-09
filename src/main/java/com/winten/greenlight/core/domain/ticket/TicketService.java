package com.winten.greenlight.core.domain.ticket;

import com.github.f4b6a3.ulid.Ulid;
import com.winten.greenlight.core.api.controller.v2.ticket.TicketIssueRequest;
import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketRepository;
import com.winten.greenlight.core.domain.action.DefaultRuleType;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.room.RoomRule;
import com.winten.greenlight.core.domain.room.RoomService;
import com.winten.greenlight.core.domain.site.SiteOperationStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import com.winten.greenlight.core.support.util.JwtUtil;
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
    private static final Duration TICKET_TTL = Duration.ofMinutes(180);

    private final RedisKeyBuilder redisKeyBuilder;
    private final QueueRepository queueRepository;
    private final TicketRepository ticketRepository;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final TicketConverter ticketConverter;
    private final JwtUtil jwtUtil;

    public Mono<Ticket> issueWaitingTicket(TicketIssueRequest request, String apiKey) {
        var score = System.currentTimeMillis();
        return roomService.findRoomById(request.getRoomId())
                .flatMap(room -> {
                    var ticketId = generateNewTicketId();
                    // TODO room apikey 검증하기
                    return roomService.findSiteOperationStatus(room.getSiteId())
                        .defaultIfEmpty(new SiteOperationStatus(true, false))
                        .flatMap(siteStatus -> {
                            if (!siteStatus.siteEnabled()) {
                                return Mono.error(CoreException.of(ErrorCode.SITE_DISABLED));
                            }
                            if (!siteStatus.queueEnabled()
                                    || !room.getEnabled()
                                    || !matchesRoomRule(room, request.getRuleParameter())) {
                                String token = jwtUtil.encode(ticketId, WaitStatus.BYPASSED);
                                return Mono.just(Ticket.bypassed(room.getRoomId(), ticketId, token));
                            }
                            return processWaitingTicket(room, ticketId, score);
                        });
                });
    }

    // rule에 맞춰 대기열 타야하는지 검사
    private boolean matchesRoomRule(Room room, String parameter) {
        if (room.getDefaultRuleType() == DefaultRuleType.ALL) { // ALL → 무조건 대기열 타야함
            return true;
        }

        boolean isInclude = room.getDefaultRuleType() == DefaultRuleType.INCLUDE;

        if (parameter == null || parameter.isBlank()) {
            return !isInclude; // INCLUDE → false (paramter가 없으니 무조건 대기열 안탐), EXCLUDE → true (paramter가 없으니 무조건 대기열 타야함)
        }

        for (RoomRule rule : room.getRoomRules()) {
            var targetValue = rule.getValue();
            boolean matched = switch (rule.getMatchOperator()) {
                case EQUAL    -> parameter.equals(targetValue);
                case CONTAINS -> parameter.contains(targetValue);
            };

            if (matched) { // 특정 조건에 맞는 경우
                return isInclude; // INCLUDE → true(대기열 타야함), EXCLUDE → false(대기열 제외)
            }
        }

        // 조건에 맞지 않은 경우
        return !isInclude; // INCLUDE → false, EXCLUDE → true
    }

    private String generateNewTicketId() {
        return Ulid.fast().toLowerCase();
    }


    private Mono<Ticket> processWaitingTicket(Room room, String ticketId, long score) {
        String roomId = room.getRoomId();
        long heartbeatScore = System.currentTimeMillis() - 3000;
        long metricBucket = calculateMetricCounterBucket();

        return roomRepository.enqueueIssuedTicket(
                        roomId,
                        ticketId,
                        room.getCapacity(),
                        score,
                        heartbeatScore,
                        metricBucket
                )
                .flatMap(nextStatus -> {
                    var ticket = Ticket.builder()
                            .roomId(roomId)
                            .ticketId(ticketId)
                            .remainingUses(1)
                            .timestamp(score)
                            .waitStatus(nextStatus)
                            .greenlightToken(jwtUtil.encode(ticketId, nextStatus))
                            .build();
                    return saveTicket(ticket).thenReturn(ticket);
                });
    }

    private Mono<Boolean> saveTicket(Ticket ticket) {
        return ticketRepository.saveTicket(ticketConverter.toEntity(ticket), TICKET_TTL);
    }

    public Mono<TicketVerification> verifyTicket(String ticketId, String hashParam) {
        return ticketRepository.findTicketById(ticketId)
            .defaultIfEmpty(new Ticket())
            .flatMap(ticket -> {
                if (ticket.getTicketId() == null) { // 예외케이스, 대기가 완료되지 않은 경우
                    return Mono.just(TicketVerification.fail(ticketId, "대기 ID가 유효하지 않습니다.")); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                }
                if (!hashParam.equals(ticket.getGreenlightToken())) {
                    return Mono.just(TicketVerification.fail(ticketId, "TicketId 또는 hash가 유효하지 않습니다."));
                }
                return ticketRepository.findHeartbeatPosition(ticket.getRoomId(), ticketId, WaitStatus.ENTERED)
                        .defaultIfEmpty(-1L)
                        .flatMap(rank -> {
                            if (rank == -1) {
                                return Mono.just(TicketVerification.fail(ticketId, "TicketId를 찾을 수 없거나 대기가 완료되지 않았습니다."));
                            }
                            var now = System.currentTimeMillis();
                            ticket.setTimestamp(now);
                            ticket.setWaitTimeMs(now - ticket.getTimestamp()); // 고객의 score와 지금 시간차만큼 대기한 것으로 판단

                            String roomKey = redisKeyBuilder.roomQueue(ticket.getRoomId(), WaitStatus.ENTERED);
                            return queueRepository.add(roomKey, ticketId, System.currentTimeMillis())
                                    .map(_ -> TicketVerification.success(ticketId, ticket.getRoomId()))
                                    .doOnNext(_ -> {
                                        Mono.when(
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
            });
    }

    public Mono<TicketStatus> getTicketStatus(String ticketId, String greenlightToken) {
        return ticketRepository.findTicketById(ticketId)
                .switchIfEmpty(Mono.error(new CoreException(ErrorCode.TICKET_NOT_FOUND, "존재하지 않는 Ticket ID입니다: " + ticketId)))
                .flatMap(ticket ->  getTicketStatus(ticket)
                            .flatMap(status -> {
                                if (status.getWaitStatus() != WaitStatus.WAITING) {
                                    return Mono.just(status);
                                }
                                return this.refreshHeartbeatToNow(ticket.getRoomId(), ticketId, WaitStatus.WAITING)
                                        .thenReturn(status);
                            })
                );
    }

    public long calculateRetryAfterFromPosition(long position) {
        if (position < 10) return 1;
        if (position < 50) return 2;
        if (position < 100) return 3;
        if (position < 1000) return 4 + (position - 100) * 7 / 900; // 100 -> 3초, 1000 직전 -> 9초대
        if (position < 10000) return 10 + (position - 1000) * 20 / 9000;
        return 30;
    }

    private Mono<TicketStatus> getTicketStatus(Ticket ticket) {
        var roomId = ticket.getRoomId();
        var ticketId = ticket.getTicketId();

        return ticketRepository.findQueuePosition(roomId, ticketId, WaitStatus.WAITING)
                .defaultIfEmpty(-1L) // 1. Empty일 경우 -1로 취급
                .flatMap(rank -> {
                    // [CASE 1] WAITING 상태인 경우 (대기열 정보 계산)
                    if (rank != -1L) {
                        return roomRepository.getLatestRoomMetric(roomId)
                                .flatMap(metric -> roomRepository.countWaitingCustomersInRoom(roomId)
                                        .map(totalCount -> {
                                            long position = rank + 1; // 0-based -> 1-based
                                            long behindCount = Math.max(totalCount - position, 0);
                                            // TODO: 예상 대기 시간 계산 로직 적용
                                            long estimatedWaitTime = calculateEstimatedWaitTime(metric.getRoomCapacity(), position, metric.getRecentlyExited());

                                            return TicketStatus.builder()
                                                    .ticketId(ticketId)
                                                    .position(position)
                                                    .behindCount(behindCount)
                                                    .estimatedWaitTime(estimatedWaitTime)
                                                    .waitStatus(WaitStatus.WAITING)
                                                    .retryAfter(calculateRetryAfterFromPosition(position)) // TODO 예상 대기시간 추가
                                                    .build();
                                        }));
                    }

                    // [CASE 2] WAITING이 아닌 경우 -> ENTERED 상태 확인
                    return ticketRepository.findHeartbeatPosition(roomId, ticketId, WaitStatus.ENTERED)
                            .defaultIfEmpty(-1L)
                            .flatMap(enteredRank -> {
                                if (enteredRank != -1L) {
                                    String greenlightToken = jwtUtil.encode(ticketId, WaitStatus.ENTERED);

                                    // ENTERED 상태인 경우 (대기열 정보는 0이나 null로 처리하거나 상태만 반환)
                                    return Mono.just(TicketStatus.builder()
                                            .ticketId(ticketId)
                                            .waitStatus(WaitStatus.ENTERED)
                                            .greenlightToken(greenlightToken)
                                            .build()
                                    );
                                }

                                // [CASE 3] 어디에도 없는 경우 -> 에러 반환
                                return Mono.error(new CoreException(ErrorCode.INVALID_TICKET_STATE, "대기열에서 티켓을 찾을 수 없습니다."));
                            });
                });

    }

    private long calculateEstimatedWaitTime(long capacity, long position, long recentlyExited) {
        if (capacity <= 0) { // capacity가 0보다 작으면 입장불가
            return -1;
        }
        // 1. totalActive가 capacity 보다 작으면 바로입장 가능 (즉시 진입)
        long remainder = position - capacity;
        if (remainder <= 0) {
            return 0;
        }
        if (recentlyExited < capacity * 3) { // 2. recentlyExited가 capacity의 30% 미만이면 평균 30초 머무는 것으로 계산
            // recentlyExited는 3분간 나간 전체 사용자 수. 30초 머무는 상황이므로 capacity가 1일 때 3분동안 6명이 나감.
            recentlyExited = Math.round(capacity * 2.1 + recentlyExited * 0.3);
        }
        long estimatedWaitTime = (remainder * 180) / recentlyExited;

        // return Math.max(estimatedWaitTime, 1);
        return Math.max(estimatedWaitTime / 2, 1); // 26.09.03 예외적으로 대기시간 50% 보정
    }


    public Mono<Boolean> updateHeartbeatFromTicketId(String ticketId, WaitStatus heartbeatType) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(ticket -> this.refreshHeartbeatToNow(ticket.getRoomId(), ticketId, heartbeatType))
                .switchIfEmpty(Mono.error(new CoreException(ErrorCode.TICKET_NOT_FOUND, "존재하지 않는 Ticket ID입니다: " + ticketId)));
    }

    private Mono<Boolean> refreshHeartbeatToNow(String roomId, String ticketId, WaitStatus heartbeatType) {
        var score = System.currentTimeMillis() - 3000;
        return roomRepository.refreshHeartbeatScore(roomId, ticketId, heartbeatType, score);
    }

    private long calculateMetricCounterBucket() {
        long currentBucketStart = (System.currentTimeMillis() / 3000) * 3000;
        return currentBucketStart - 3000;
    }

    public Mono<Void> deleteTicket(String ticketId, WaitStatus heartbeatType) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(ticket -> {
                    String roomId = ticket.getRoomId();
                    return Mono.zip(
                                roomRepository.deleteHeartbeat(roomId, ticketId, heartbeatType),
                                roomRepository.deleteQueue(roomId, ticketId, heartbeatType)
                            )
                            .doOnError(e -> log.error("deleteHeartbeat failed. ticketId: {}, reason: {}", ticketId, e.getMessage()))
                            .filter(tuple -> tuple.getT1() > 0)
                            .flatMap(ignored -> {
                                long targetBucket = calculateMetricCounterBucket();

                                // 1. Metric Count 증가 작업
                                var metricType = heartbeatType == WaitStatus.WAITING ? WaitStatus.CANCELLED : WaitStatus.EXITED; // 대기 중 이탈했다면 CANCELLED, 아니라면 EXITED
                                Mono<Void> increaseMetricTask = roomRepository.increaseMetricCount(roomId, metricType, targetBucket)
                                        .doOnError(e -> log.error("increaseMetricCount failed. ticketId: {}, reason: {}", ticketId, e.getMessage()))
                                        .then();

                                return Mono.whenDelayError(increaseMetricTask)
                                        .subscribeOn(Schedulers.boundedElastic());
                            });
                })
                .then();
    }


    // TODO 이게 뭐지...?
//    private boolean verifyHash(String raw, String actual) {
//        if (Objects.equals(raw, actual)) {
//            return Mono.just(TicketVerification.fail(ticketId, "TicketId 또는 hash가 유효하지 않습니다."));
//        }
//    }
}
