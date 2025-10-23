package com.winten.greenlight.core.support.logging;

import com.winten.greenlight.core.support.error.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class Log implements Serializable {
    private LocalDateTime timestamp;
    private String level;
    private String message;
    private SystemType systemType;
    private String userId;
    private String userIp;

    public Log(LocalDateTime timestamp, LogLevel level, String message, SystemType systemType, String userId, String userIp) {
        this.timestamp = timestamp;
        this.level = level.name();
        this.message = message;
        this.systemType = systemType;
        this.userId = userId;
        this.userIp = userIp;
    }

    public Map<String, String> toMap() {
        final Map<String, String> map = new HashMap<>();
        map.put("timestamp", timestamp.toString());
        map.put("level", level);
        map.put("message", message);
        map.put("systemType", systemType.toString());
        map.put("userId", userId);
        map.put("userIp", userIp);
        return map;
    }
}