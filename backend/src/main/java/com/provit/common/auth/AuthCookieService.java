package com.provit.common.auth;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 인증 토큰을 JavaScript가 접근할 수 있는 브라우저 저장소에 보관하지 않습니다.
 */
@Component
public class AuthCookieService {

    @Value("${auth.cookie.name:provit_access}")
    private String cookieName;

    // 로컬 Tomcat은 HTTP를 사용합니다. Secure 쿠키는 HTTPS가 필요하므로,
    // 운영 환경에서는 Git에서 제외한 api.properties에서 true로 설정해야 합니다.
    @Value("${auth.cookie.secure:false}")
    private boolean secure;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    public Optional<String> getAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public ResponseCookie createAccessTokenCookie(String token, long maxAgeMillis) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
