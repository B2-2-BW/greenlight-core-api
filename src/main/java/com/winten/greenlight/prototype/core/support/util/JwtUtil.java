package com.winten.greenlight.prototype.core.support.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    // TODO
    // UserInfo로부터 JWT 토큰 생성
//    public Customer generateToken(User user) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("accountId", user.getAccountId());
//        claims.put("userId", user.getUserId());
//        claims.put("userRole", user.getUserRole());
//
//        return new UserToken(createToken(claims, user.getUserId()));
//    }

    // Claims와 subject로 토큰 생성
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 토큰에서 사용자명 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 토큰에서 만료일 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 토큰에서 특정 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 토큰에서 모든 클레임 추출
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 만료 확인
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    // TODO
    // 토큰 유효성 검증
//    public Boolean validateToken(String token, CurrentUser currentUser) {
//        final String username = extractUsername(token);
//        return (username.equals(currentUser.getUserId()) && !isTokenExpired(token));
//    }

    // 토큰 유효성 검증 (UserInfo 없이)
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    // TODO
    // 토큰에서 UserInfo 객체 생성
//    public CurrentUser getCurrentUserFromToken(String token) {
//        Claims claims = extractAllClaims(token);
//
//        UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));
//        return CurrentUser.builder()
//                .accountId(claims.get("accountId", Long.class))
//                .userId(claims.get("userId", String.class))
//                .userRole(userRole)
//                .build();
//    }

    // 토큰에서 특정 클레임 값 추출
    public String getClaimFromToken(String token, String claimName) {
        Claims claims = extractAllClaims(token);
        return claims.get(claimName, String.class);
    }

    // 토큰 만료까지 남은 시간 (밀리초)
    public Long getExpirationTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }
}