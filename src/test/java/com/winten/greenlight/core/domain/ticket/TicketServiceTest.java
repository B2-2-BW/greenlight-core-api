package com.winten.greenlight.core.domain.ticket;

import com.winten.greenlight.core.api.controller.v2.ticket.TicketIssueRequest;
import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketRepository;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketEntity;
import com.winten.greenlight.core.domain.action.DefaultRuleType;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.Room;
import com.winten.greenlight.core.domain.room.RoomService;
import com.winten.greenlight.core.domain.site.SiteOperationStatus;
import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorCode;
import com.winten.greenlight.core.support.util.JwtUtil;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private RedisKeyBuilder redisKeyBuilder;
    @Mock
    private QueueRepository queueRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private RoomService roomService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TicketConverter ticketConverter;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void issueWaitingTicketRejectsDisabledSiteBeforeRoomBypass() {
        var room = room(false);
        when(roomService.findRoomById("room-1")).thenReturn(Mono.just(room));
        when(roomService.findSiteOperationStatus("site-1"))
                .thenReturn(Mono.just(new SiteOperationStatus(false, true)));

        StepVerifier.create(ticketService.issueWaitingTicket(new TicketIssueRequest("room-1", null), "api-key"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(CoreException.class);
                    var errorCode = ((CoreException) error).getErrorCode();
                    assertThat(errorCode).isEqualTo(ErrorCode.SITE_DISABLED);
                    assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .verify();

        verify(jwtUtil, never()).encode(anyString(), any());
        verify(roomRepository, never()).enqueueIssuedTicket(anyString(), anyString(), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void issueWaitingTicketBypassesWhenSiteQueueIsDisabled() {
        var room = room(true);
        when(roomService.findRoomById("room-1")).thenReturn(Mono.just(room));
        when(roomService.findSiteOperationStatus("site-1"))
                .thenReturn(Mono.just(new SiteOperationStatus(true, false)));
        when(jwtUtil.encode(anyString(), eq(WaitStatus.BYPASSED))).thenReturn("bypass-token");

        StepVerifier.create(ticketService.issueWaitingTicket(new TicketIssueRequest("room-1", null), "api-key"))
                .assertNext(ticket -> {
                    assertThat(ticket.getWaitStatus()).isEqualTo(WaitStatus.BYPASSED);
                    assertThat(ticket.getGreenlightToken()).isEqualTo("bypass-token");
                })
                .verifyComplete();

        verify(roomRepository, never()).enqueueIssuedTicket(anyString(), anyString(), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void issueWaitingTicketContinuesNormallyWhenSiteAndQueueAreEnabled() {
        var room = room(true);
        when(roomService.findRoomById("room-1")).thenReturn(Mono.just(room));
        when(roomService.findSiteOperationStatus("site-1"))
                .thenReturn(Mono.just(new SiteOperationStatus(true, true)));
        when(roomRepository.enqueueIssuedTicket(eq("room-1"), anyString(), eq(10), anyLong(), anyLong(), anyLong()))
                .thenReturn(Mono.just(WaitStatus.WAITING));
        when(jwtUtil.encode(anyString(), eq(WaitStatus.WAITING))).thenReturn("waiting-token");
        when(ticketConverter.toEntity(any(Ticket.class))).thenReturn(new TicketEntity());
        when(ticketRepository.saveTicket(any(TicketEntity.class), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(ticketService.issueWaitingTicket(new TicketIssueRequest("room-1", null), "api-key"))
                .assertNext(ticket -> {
                    assertThat(ticket.getWaitStatus()).isEqualTo(WaitStatus.WAITING);
                    assertThat(ticket.getGreenlightToken()).isEqualTo("waiting-token");
                })
                .verifyComplete();
    }

    @Test
    void issueWaitingTicketEntersImmediatelyWhenLuaReturnsEntered() {
        var room = room(true);
        when(roomService.findRoomById("room-1")).thenReturn(Mono.just(room));
        when(roomService.findSiteOperationStatus("site-1"))
                .thenReturn(Mono.just(new SiteOperationStatus(true, true)));
        when(roomRepository.enqueueIssuedTicket(eq("room-1"), anyString(), eq(10), anyLong(), anyLong(), anyLong()))
                .thenReturn(Mono.just(WaitStatus.ENTERED));
        when(jwtUtil.encode(anyString(), eq(WaitStatus.ENTERED))).thenReturn("entered-token");
        when(ticketConverter.toEntity(any(Ticket.class))).thenReturn(new TicketEntity());
        when(ticketRepository.saveTicket(any(TicketEntity.class), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(ticketService.issueWaitingTicket(new TicketIssueRequest("room-1", null), "api-key"))
                .assertNext(ticket -> {
                    assertThat(ticket.getWaitStatus()).isEqualTo(WaitStatus.ENTERED);
                    assertThat(ticket.getGreenlightToken()).isEqualTo("entered-token");
                })
                .verifyComplete();
    }

    private Room room(boolean enabled) {
        return Room.builder()
                .roomId("room-1")
                .siteId("site-1")
                .capacity(10)
                .enabled(enabled)
                .defaultRuleType(DefaultRuleType.ALL)
                .roomRules(List.of())
                .build();
    }

    @Test
    void deleteTicketIncreasesExitedMetricWhenHeartbeatWasDeleted() {
        var ticket = Ticket.builder()
                .ticketId("ticket-1")
                .roomId("room-1")
                .build();
        when(ticketRepository.findTicketById("ticket-1")).thenReturn(Mono.just(ticket));
        when(roomRepository.deleteHeartbeat("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(1L));
        when(roomRepository.deleteQueue("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(1L));
        when(roomRepository.increaseMetricCount(eq("room-1"), eq(WaitStatus.EXITED), anyLong()))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(ticketService.deleteTicket("ticket-1", WaitStatus.ENTERED))
                .verifyComplete();

        verify(roomRepository).increaseMetricCount(eq("room-1"), eq(WaitStatus.EXITED), anyLong());
    }

    @Test
    void deleteTicketDoesNotIncreaseExitedMetricWhenOnlyQueueWasDeleted() {
        var ticket = Ticket.builder()
                .ticketId("ticket-1")
                .roomId("room-1")
                .build();
        when(ticketRepository.findTicketById("ticket-1")).thenReturn(Mono.just(ticket));
        when(roomRepository.deleteHeartbeat("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(0L));
        when(roomRepository.deleteQueue("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(1L));

        StepVerifier.create(ticketService.deleteTicket("ticket-1", WaitStatus.ENTERED))
                .verifyComplete();

        verify(roomRepository, never()).increaseMetricCount(eq("room-1"), eq(WaitStatus.EXITED), anyLong());
    }

    @Test
    void deleteTicketDoesNotIncreaseExitedMetricWhenNothingWasDeleted() {
        var ticket = Ticket.builder()
                .ticketId("ticket-1")
                .roomId("room-1")
                .build();
        when(ticketRepository.findTicketById("ticket-1")).thenReturn(Mono.just(ticket));
        when(roomRepository.deleteHeartbeat("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(0L));
        when(roomRepository.deleteQueue("room-1", "ticket-1", WaitStatus.ENTERED)).thenReturn(Mono.just(0L));

        StepVerifier.create(ticketService.deleteTicket("ticket-1", WaitStatus.ENTERED))
                .verifyComplete();

        verify(roomRepository, never()).increaseMetricCount(eq("room-1"), eq(WaitStatus.EXITED), anyLong());
    }
}
