package com.winten.greenlight.prototype.core.support.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMemberBuilder {

    public String queue(Long actionId, String customerId) {
        return String.format("%d:%s", actionId, customerId);
    }

}