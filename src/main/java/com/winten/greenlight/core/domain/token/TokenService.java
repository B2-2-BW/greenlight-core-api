package com.winten.greenlight.core.domain.token;

import com.winten.greenlight.core.db.repository.redis.token.TokenRepository;
import com.winten.greenlight.core.domain.customer.CustomerEntry;
import com.winten.greenlight.core.domain.action.Action;
import com.winten.greenlight.core.support.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 토큰(JWT) 도메인의 비즈니스 로직을 담당하는 서비스입니다.
 * 토큰 발급, 만료, 유효성 검증 등의 기능을 제공합니다.
 * 위치: com.winten.greenlight.core.domain.token
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtUtil jwtUtil;

    /**
     * 새로운 JWT 토큰을 발급하고, 관련 메타데이터(고객 ID, 액션 ID, 상태 등)를 Redis에 저장합니다.
     * <b>(시나리오 1 적용)</b> 발급 전, 해당 고객과 액션에 대한 기존 토큰이 있다면 먼저 만료시킵니다.
     *
     * @param customerId 고객 ID
     * @param action     관련 Action 정보
     * @return Mono<String> 발급된 JWT 토큰 문자열
     */
    public Mono<String> issueToken(String customerId, Action action, String destinationUrl) {
        var entry = CustomerEntry.builder()
                .actionGroupId(action.getActionGroupId())
                .actionId(action.getId())
                .customerId(customerId)
                .destinationUrl(destinationUrl) // 이 줄을 추가합니다.
                .timestamp(System.currentTimeMillis())
                .build();
        return Mono.just(jwtUtil.generateToken(entry));
    }
}