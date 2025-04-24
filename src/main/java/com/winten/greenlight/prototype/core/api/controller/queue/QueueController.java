package com.winten.greenlight.prototype.core.api.controller.customer;

import com.winten.greenlight.prototype.core.support.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/queue")
public class QueueController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 1. 대기열 진입 및 토큰 발급
    @PostMapping("/enter")
    public ResponseEntity<?> enterQueue(@RequestBody Map<String, String> payload) {
        String queueId = UUID.randomUUID().toString();
        String actionId = payload.getOrDefault("actionId", "event123");
        long score = System.currentTimeMillis();

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(queueId, actionId, score);

        // Redis 저장
        redisTemplate.opsForZSet().add("queue", queueId, score);
        redisTemplate.opsForValue().set("session:" + queueId, token, 30, TimeUnit.MINUTES);

        // 쿠키 설정
        ResponseCookie cookie = ResponseCookie.from("queueToken", token)
            .httpOnly(true)
            .path("/")
            .maxAge(30 * 60)
            .secure(true)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(Map.of("message", "Entered queue", "queueId", queueId));
    }

    // 2. 대기 완료 및 리다이렉트
    @PostMapping("/complete")
    public ResponseEntity<?> completeQueue(@RequestBody Map<String, String> payload) {
        String queueId = payload.get("queueId");
        String token = redisTemplate.opsForValue().get("session:" + queueId);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }

        // 대기열 제거
        redisTemplate.opsForZSet().remove("queue", queueId);

        // 쿠키 재설정
        ResponseCookie cookie = ResponseCookie.from("queueToken", token)
            .httpOnly(true)
            .path("/")
            .maxAge(30 * 60)
            .secure(true)
            .build();

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.LOCATION, "https://api-gateway.thehyundai.com")
            .build();
    }

    // 3. 이탈 처리
    @PostMapping("/leave")
    public ResponseEntity<?> leaveQueue(@CookieValue("queueToken") String token) {
        try {
            Claims claims = jwtUtil.validateToken(token);
            String queueId = claims.getSubject();

            // Redis 데이터 삭제
            redisTemplate.opsForZSet().remove("queue", queueId);
            redisTemplate.delete("session:" + queueId);
            redisTemplate.opsForZSet().remove("activeUsers", queueId);
            redisTemplate.opsForZSet().remove("concurrentUsers", queueId);

            // 쿠키 무효화
            ResponseCookie cookie = ResponseCookie.from("queueToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .secure(true)
                .build();

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Left queue successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    // 4. 하트비트 처리 (Interceptor에서 활성화 체크)
    @PostMapping("/heartbeat")
    public ResponseEntity<?> handleHeartbeat(@RequestBody Map<String, String> payload) {
        // Interceptor에서 처리됨
        return ResponseEntity.ok().build();
    }

    // 5. 활동 처리 (Interceptor에서 활성화 체크)
    @PostMapping("/activity")
    public ResponseEntity<?> handleActivity(@RequestBody Map<String, String> payload) {
        // Interceptor에서 처리됨
        return ResponseEntity.ok().build();
    }

    // 6. 통계 조회
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Long concurrent = redisTemplate.opsForZSet().zCard("concurrentUsers");
        Long active = redisTemplate.opsForZSet().zCard("activeUsers");
        return ResponseEntity.ok(Map.of("concurrentUsers", concurrent != null ? concurrent : 0,
            "activeUsers", active != null ? active : 0));
    }

    // 7. 활성화 여부 확인
    @PostMapping("/check-activation")
    public ResponseEntity<?> checkActivation(@RequestBody Map<String, String> payload) {
        String token = payload.get("queueToken");
        try {
            Claims claims = jwtUtil.validateToken(token);
            String queueId = claims.getSubject();
            Double activeScore = redisTemplate.opsForZSet().score("activeUsers", queueId);
            boolean isActive = activeScore != null && (System.currentTimeMillis() - activeScore) < 5 * 60 * 1000;
            return ResponseEntity.ok(Map.of("queueId", queueId, "isActive", isActive));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

}
