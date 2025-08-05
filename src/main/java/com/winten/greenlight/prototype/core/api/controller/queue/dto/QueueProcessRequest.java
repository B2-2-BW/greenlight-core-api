package com.winten.greenlight.prototype.core.api.controller.queue.dto;

import java.util.Map;

public record QueueProcessRequest(
    Long actionId,
    String destinationUrl,
    String greenlightToken,
    Map<String, String> requestParams,
    long timestamp
) {}
