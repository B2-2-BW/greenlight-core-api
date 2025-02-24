package com.winten.greenlight.prototype.core.support.logging;

import com.winten.greenlight.prototype.core.support.error.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Log implements Serializable {
    private LocalDateTime timestamp;
    private String level;
    private String message;
    private LogContext context;


    public static Log of(LogLevel level, final String message, final String userId, final String userIp) {
        return new Log(LocalDateTime.now(), level.name(), message, LogContext.from(userId, userIp));
    }
    public static Log info(final String message, final String userId, final String userIp) {
        return Log.of(LogLevel.INFO, message, userId, userIp);
    }
    public static Log warn(final String message, final String userId, final String userIp) {
        return Log.of(LogLevel.WARN, message, userId, userIp);
    }
    public static Log error(final String message, final String userId, final String userIp) {
        return Log.of(LogLevel.ERROR, message, userId, userIp);
    }
}