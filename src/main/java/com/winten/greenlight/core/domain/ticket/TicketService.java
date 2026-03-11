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
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Slf4j
@Service
public class TicketService {
    private final RedisKeyBuilder redisKeyBuilder;
    private final QueueRepository queueRepository;
    private final TicketRepository ticketRepository;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final TicketConverter ticketConverter;
    private final MessageDigest messageDigest;

    public TicketService(
            RedisKeyBuilder redisKeyBuilder,
            QueueRepository queueRepository,
            TicketRepository ticketRepository,
            RoomService roomService,
            RoomRepository roomRepository,
            TicketConverter ticketConverter
    ) throws NoSuchAlgorithmException {
        this.redisKeyBuilder = redisKeyBuilder;
        this.queueRepository = queueRepository;
        this.ticketRepository = ticketRepository;
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.ticketConverter = ticketConverter;
        messageDigest = MessageDigest.getInstance("SHA-256");
    }

    public Mono<Ticket> issueWaitingTicket(TicketIssueRequest request, String apiKey) {
        var score = System.currentTimeMillis();
        return roomService.findRoomById(request.getRoomId())
                .flatMap(room -> {
                    var ticketId = generateNewTicketId();
                    if (!room.getEnabled()) {
                        return Mono.just(Ticket.bypassed(room.getRoomId(), ticketId)); // TODO actionGroup 비활성화 되어있을 때 바로 입장 가능하도록 반환하기
                    }
                    // TODO room apikey 검증하기
                    return roomRepository.isSiteEnabled(room.getSiteId())
                        .switchIfEmpty(Mono.just(false))
                        .flatMap(enabled -> {
                            if (!enabled) {
                                return Mono.just(Ticket.bypassed(room.getRoomId(), ticketId));
                            }
                            return processWaitingTicket(room, ticketId, score);
                        });
                });
    }

    private String generateNewTicketId() {
        return Ulid.fast().toLowerCase();
    }


    private Mono<Ticket> processWaitingTicket(Room room, String ticketId, long score) {

        Mono<Boolean> waitingRequired = this.isWaitingRequired(room.getRoomId(), room.getCapacity());

        return waitingRequired.flatMap(isWaitingRequired -> {
            WaitStatus status = isWaitingRequired ? WaitStatus.WAITING : WaitStatus.ENTERED;

            String combined = ticketId + ":" + score;
            byte[] byteHash = messageDigest.digest(combined.getBytes(StandardCharsets.UTF_8));
            String hash = DatatypeConverter.printHexBinary(byteHash).toLowerCase();
            String roomId = room.getRoomId();
            var ticket = Ticket.builder()
                    .roomId(roomId)
                    .adImageUrl(room.getAdImageUrl())
                    .ticketId(ticketId)
                    .hash(hash)
                    .remainingUses(1)
                    .timestamp(score)
                    .build();

            Mono<Long> saveTicket = this.saveTicket(ticket)
                    .flatMap(_ -> roomRepository.addToRoomQueue(ticket, status));

            long targetBucket = this.calculateMetricCounterBucket();

            // 바로 입장했을 경우 heartbeat 업데이트
            Mono<Void> updateHeartbeat = (status == WaitStatus.ENTERED)
                    ? roomRepository.updateHeartbeatScore(roomId, ticketId, status)
                    .then(roomRepository.increaseMetricCount(roomId, status, targetBucket))
                    .then()
                    : Mono.empty();
            // 비동기 로깅 작업
            Mono.when(
                    roomRepository.increaseMetricCount(roomId, WaitStatus.WAITING, targetBucket), // 실시간 유입량 계산을 위한 기록 (SortedSet)
                    updateHeartbeat
            ).subscribeOn(Schedulers.boundedElastic()) // 별도 스레드에서 실행
            .subscribe(
                    null,
                    e -> log.error("[Async Log Error] failed for ticket: {}", ticketId, e) // 에러 발생 시 로그 남김
            );

            ticket.setWaitStatus(status);
            return saveTicket.thenReturn(ticket);
        });
    }

    private Mono<Boolean> saveTicket(Ticket ticket) {
        var ttl = Duration.ofMinutes(999);

        return ticketRepository.saveTicket(ticketConverter.toEntity(ticket), ttl);
    }

    private Mono<Boolean> isWaitingRequired(String roomId, int roomCapacity) {
        return roomRepository.countWaitingCustomersInRoom(roomId)
                .flatMap(waiting -> {
                    if (waiting > 0) {
                        return Mono.just(true);
                    }
                    return roomRepository.countEnteredCustomersInRoom(roomId)
                                    .map(enterCount -> (roomCapacity - enterCount) < 1);
                });
    }

    public Mono<TicketVerification> verifyTicket(String ticketId, String hash) {
        return ticketRepository.findTicketById(ticketId)
            .defaultIfEmpty(new Ticket())
            .flatMap(ticket -> {
                if (ticket.getTicketId() == null) { // 예외케이스, 대기가 완료되지 않은 경우
                    return Mono.just(TicketVerification.fail(ticketId, "대기 ID가 유효하지 않습니다.")); // 유효하지 않은 입장권인 경우 하단 switchIfEmpty에서 처리
                }
                if (!hash.equals(ticket.getHash())) {
                    return Mono.just(TicketVerification.fail(ticketId, "TicketId 또는 hash가 유효하지 않습니다."));
                }
                return ticketRepository.findQueuePosition(ticket.getRoomId(), ticketId, WaitStatus.WAITING)
                        .defaultIfEmpty(-1L)
                        .flatMap(rank -> {
                            if (rank == -1) {
                                return Mono.just(TicketVerification.fail(ticketId, "대기가 완료되지 않았습니다."));
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

    public Mono<TicketStatus> getTicketStatus(String ticketId, String hash) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(ticket -> this.buildTicketStatus(ticket, hash))
                .switchIfEmpty(Mono.error(new CoreException(ErrorCode.TICKET_NOT_FOUND, "존재하지 않는 Ticket ID입니다: " + ticketId)));
    }

    private Mono<TicketStatus> buildTicketStatus(Ticket ticket, String hash) {
        var roomId = ticket.getRoomId();
        var ticketId = ticket.getTicketId();

        return ticketRepository.findQueuePosition(roomId, ticketId, WaitStatus.WAITING)
                .defaultIfEmpty(-1L) // 1. Empty일 경우 -1로 취급
                .flatMap(rank -> {
                    // [CASE 1] WAITING 상태인 경우 (대기열 정보 계산)
                    if (rank != -1L) {
                        return roomRepository.countWaitingCustomersInRoom(roomId)
                                .map(totalCount -> {
                                    long myPosition = rank + 1; // 0-based -> 1-based
                                    long behindCount = Math.max(totalCount - myPosition, 0);
                                    // TODO: 예상 대기 시간 계산 로직 적용
                                    long estimatedWaitTime = 0L;

                                    return TicketStatus.builder()
                                            .ticketId(ticketId)
                                            .position(myPosition)
                                            .behindCount(behindCount)
                                            .estimatedWaitTime(estimatedWaitTime)
                                            .waitStatus(WaitStatus.WAITING) // 필요 시 상태 필드 추가
                                            .build();
                                });
                    }

                    // [CASE 2] WAITING이 아닌 경우 -> ENTERED 상태 확인
                    return ticketRepository.findQueuePosition(roomId, ticketId, WaitStatus.ENTERED)
                            .defaultIfEmpty(-1L)
                            .flatMap(enteredRank -> {
                                if (enteredRank != -1L) {
                                    // ENTERED 상태인 경우 (대기열 정보는 0이나 null로 처리하거나 상태만 반환)
                                    return Mono.just(TicketStatus.builder()
                                            .ticketId(ticketId)
                                            .waitStatus(WaitStatus.ENTERED)
                                            .build());
                                }

                                // [CASE 3] 어디에도 없는 경우 -> 에러 반환
                                return Mono.error(new CoreException(ErrorCode.INVALID_TICKET_STATE, "대기열에서 티켓을 찾을 수 없습니다."));
                            });
                });

    }

    public Mono<Boolean> updateHeartbeat(String ticketId, WaitStatus heartbeatType) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(ticket -> roomRepository.updateHeartbeatScore(ticket.getRoomId(), ticketId, heartbeatType))
                .switchIfEmpty(Mono.error(new CoreException(ErrorCode.TICKET_NOT_FOUND, "존재하지 않는 Ticket ID입니다: " + ticketId)));
    }

    private long calculateMetricCounterBucket() {
        long currentBucketStart = (System.currentTimeMillis() / 3000) * 3000;
        return currentBucketStart - 3000;
    }

    public Mono<Void> deleteTicket(String ticketId, WaitStatus heartbeatType) {
        return ticketRepository.findTicketById(ticketId)
                .flatMap(ticket -> {
                    String roomId = ticket.getRoomId();
                    return roomRepository.deleteHeartbeat(roomId, ticketId, heartbeatType)
                            .doOnError(e -> log.error("deleteHeartbeat failed. ticketId: {}, reason: {}", ticketId, e.getMessage()))
                            .filter(deletedCount -> deletedCount > 0)
                            .flatMap(ignored -> {
                                long targetBucket = calculateMetricCounterBucket();

                                // 1. Metric Count 증가 작업
                                Mono<Void> increaseMetricTask = roomRepository.increaseMetricCount(roomId, WaitStatus.EXITED, targetBucket)
                                        .doOnError(e -> log.error("increaseMetricCount failed. ticketId: {}, reason: {}", ticketId, e.getMessage()))
                                        .then();

                                // 3. 두 작업을 병렬로 묶어서 실행
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