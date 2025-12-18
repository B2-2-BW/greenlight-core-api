package com.winten.greenlight.core.support.util;

public class CustomerUtil {
    public static Long parseActionIdFromCustomerId(String customerId) {
        try {
            return Long.parseLong(customerId.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }
}