package com.winten.greenlight.core.support.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.*;

public class UtcToKstLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText(); // "2025-12-09T04:26:44.649"
        // 1) LocalDateTime 으로 먼저 파싱 (타임존 없음)
        LocalDateTime utcLdt = LocalDateTime.parse(text); // ISO-8601 기본 포맷이라 formatter 필요 없음
        // 2) 이 값을 "UTC 기준"이라고 보고 KST 로 변환
        ZonedDateTime kstZdt = utcLdt.atZone(ZoneOffset.UTC).withZoneSameInstant(KST);
        return kstZdt.toLocalDateTime();
    }
}