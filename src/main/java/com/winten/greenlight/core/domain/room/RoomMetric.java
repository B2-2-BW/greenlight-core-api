package com.winten.greenlight.core.domain.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomMetric {
    private String roomId;
    private int roomCapacity;
    private long totalWaiting;
    private long totalActive;
    private long recentlyExited;
    private long waitingCount;
    private long enteredCount;
    private long exitedCount;
    private double waitingRate;
    private double enteredRate;
    private double exitedRate;
    private long estimatedWaitTime;
}