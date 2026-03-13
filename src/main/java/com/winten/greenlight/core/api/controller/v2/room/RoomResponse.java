package com.winten.greenlight.core.api.controller.v2.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private String roomId;
    private Boolean enabled;
    private String adImageUrl;
}