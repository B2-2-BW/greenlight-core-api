package com.winten.greenlight.prototype.core.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping("")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("ok"));
    }
}