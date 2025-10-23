package com.winten.greenlight.core.api.controller;

import com.winten.greenlight.core.support.error.CoreException;
import com.winten.greenlight.core.support.error.ErrorType;
import com.winten.greenlight.core.support.logging.Log;
import com.winten.greenlight.core.support.logging.LogManager;
import com.winten.greenlight.core.support.logging.LogRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("logs")
@RequiredArgsConstructor
public class LoggingController {
    private final LogManager logManager;

    @PostMapping("")
    public Mono<Log> insertLog(@RequestBody LogRequestDto requestDto, ServerHttpRequest request) {
        return Mono.defer(() -> {
            Log log = requestDto.toLog();
            var ip = request.getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddress() != null
                        ? request.getRemoteAddress().getAddress().getHostAddress()
                        : "";
            }
            log.setUserIp(ip);
            return logManager.push(log)
                    .onErrorMap(e -> new CoreException(ErrorType.INTERNAL_SERVER_ERROR, "unknown error " + e));
        });
    }
}