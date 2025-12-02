package com.winten.greenlight.core.api.controller.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LandingRequest {
    private String landingId;
    private String destinationUrl;
}