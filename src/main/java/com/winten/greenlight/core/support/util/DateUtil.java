package com.winten.greenlight.core.support.util;

import java.time.LocalDateTime;

public class DateUtil {
    public static boolean isBetweenNow(LocalDateTime from, LocalDateTime to) {
        // null 이 하나라도 있으면 false
        if (from == null || to == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(from) || now.isEqual(from)) && (now.isBefore(to) || now.isEqual(to));
    }
}