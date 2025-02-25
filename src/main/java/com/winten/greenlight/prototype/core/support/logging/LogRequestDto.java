package com.winten.greenlight.prototype.core.support.logging;

import com.winten.greenlight.prototype.core.support.error.LogLevel;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LogRequestDto {
    private LogLevel level;
    private String message;
    private SystemType systemType;

    public Log toLog() {
        return new Log(LocalDateTime.now(), level, message, systemType, null, null);
    }
}