package com.provit.util.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.UserDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JJWT 0.12.6 기반 JWT 생성, 검증 및 클레임 추출 유틸리티
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.secret:}")
    private String secretKeyPlain;

    @Value("${jwt.expiration:86400000}")
    private long expirationTime; // 밀리초 (기본 24시간)

    private SecretKey secretKey;

    // 비밀번호 변경 뒤 기존 토큰을 즉시 차단하기 위한 사용자별 발급 기준 시각이다.
    private final UserDAO userDAO;

    @Autowired
    public JwtProvider(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @PostConstruct
    public void init() {
        if (secretKeyPlain == null || secretKeyPlain.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret은 32바이트 이상의 안전한 값으로 반드시 설정해야 합니다.");
        }

        // HMAC-SHA256 알고리즘에 필요한 비밀키 생성
        byte[] keyBytes = secretKeyPlain.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 회원 정보를 바탕으로 JWT Access Token 생성 (비밀번호 제외)
     */
    public String createToken(UserDTO user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserNum()))
                .claim("email", user.getUserEmail())
                .claim("name", user.getUserName())
                .claim("nickname", user.getUserNickname())
                .claim("role", user.getUserType() != null ? user.getUserType() : "USER")
                // DB의 버전과 다르면 비밀번호 변경·탈퇴 전 발급된 토큰으로 판단한다.
                .claim("tokenVersion", user.getUserTokenVersion() != null ? user.getUserTokenVersion() : 0)
                // DB의 버전과 다르면 비밀번호 변경·탈퇴 전 발급된 토큰으로 판단한다.
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 및 만료 여부 검증 (토큰 자체는 로그에 남기지 않음)
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userNum = Long.parseLong(claims.getSubject());
            Object tokenVersionValue = claims.get("tokenVersion");
            if (!(tokenVersionValue instanceof Number)) {
                return false;
            }

            // 서버 재시작 뒤에도 유지되는 DB 버전으로 토큰을 검증한다.
            UserDTO user = userDAO.selectByUserNum(userNum);
            return user != null
                    && (user.getUserIsDeleted() == null || user.getUserIsDeleted() == 0)
                    && user.getUserTokenVersion() != null
                    && user.getUserTokenVersion().intValue() == ((Number) tokenVersionValue).intValue();
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않거나 위조된 JWT 토큰입니다: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 회원 식별 번호(USER_NUM) 추출
     */
    public Long getUserNum(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getUserEmail(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * 토큰에서 회원 권한(USER_TYPE: USER, ADMIN) 추출
     */
    public String getUserRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * 비밀번호 변경 또는 탈퇴 시 해당 사용자의 기존 JWT를 즉시 무효화한다.
     */
    /**
     * 비밀번호 변경 시점보다 먼저 발급된 해당 회원의 모든 JWT를 무효화한다.
     */
}
