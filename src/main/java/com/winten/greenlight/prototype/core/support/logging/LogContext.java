package com.winten.greenlight.prototype.core.support.logging;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class LogContext implements Serializable {
    private String userId;
    private String userIp;

    public static LogContext from(String userId, String userIp) {
        return new LogContext(userId, userIp);
    }
}