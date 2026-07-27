package com.winten.greenlight.core.domain.ticket;

import com.winten.greenlight.core.db.repository.redis.queue.QueueRepository;
import com.winten.greenlight.core.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.core.db.repository.redis.ticket.TicketRepository;
import com.winten.greenlight.core.domain.customer.WaitStatus;
import com.winten.greenlight.core.domain.room.RoomService;
import com.winten.greenlight.core.support.util.JwtUtil;
import com.winten.greenlight.core.support.util.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyLong;
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
