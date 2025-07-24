package com.winten.greenlight.prototype.core.support.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class JacksonConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1) ISO-8601 문자열로 날짜를 다루기 위한 모듈 등록
        JavaTimeModule timeModule = new JavaTimeModule();
        objectMapper.registerModule(timeModule);

        // 2) 타임스탬프 대신 문자열(예: 2024-06-12T15:00:00+09:00) 형태로 저장
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3) 기본 타임존을 한국으로 고정
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        return objectMapper;
    }
}