package com.winten.greenlight.core.support.util;

import io.hypersistence.tsid.TSID;

public class CustomerUtil {
    public static Long parseActionIdFromCustomerId(String customerId) {
        try {
            return Long.parseLong(customerId.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public static String generateCustomerId(Long actionId, String oldCustomerId) {
        // customer ID = {actionId}:{customerKey}
        try { // 기존에 사용하던 토큰이 있는 경우 customerId의 고유번호 추출
            var oldCustomerKey = parseKeyFromCustomerId(oldCustomerId);
            return makeCustomerId(actionId, oldCustomerKey);
        } catch (Exception ignored) {
            // oldCustomerId.split에 실패할 경우 새로 만듬
            return makeCustomerId(actionId, TSID.fast().toString());
        }
    }

    public static String parseKeyFromCustomerId(String customerId) {
        return customerId.split(":")[1];
    }

    public static String makeCustomerId(Long actionId, String randomSeed) {
        return actionId + ":" + randomSeed;
    }
}