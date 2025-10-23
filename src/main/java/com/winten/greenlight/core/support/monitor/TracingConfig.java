//package com.winten.greenlight.core.support.monitor;
//
//import io.micrometer.tracing.exporter.SpanExportingPredicate;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class TracingConfig {
//
//    @Bean
//    public SpanExportingPredicate ignoreActuator() {
//        return span -> {
//            String uri = span.getTags().get("uri");
//            if (uri == null) {
//                return true;
//            }
//            return !(uri.startsWith("/actuator") || uri.startsWith("/g-actuator"));
//        };
//    }
//}