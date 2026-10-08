package com.provit.controller.auth;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.provit.common.ResponseCode;
import com.provit.common.auth.AuthCookieService;
import com.provit.dao.auth.RefreshTokenDAO;
import com.provit.dto.auth.EmailSendRequestDTO;
import com.provit.dto.auth.EmailVerifyRequestDTO;
import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.MyPageUpdateRequestDTO;
import com.provit.dto.auth.PasswordResetRequestDTO;
import com.provit.dto.auth.RefreshTokenDTO;
import com.provit.dto.auth.SignupRequestDTO;
import com.provit.dto.auth.UserResponseDTO;
import com.provit.dto.auth.WithdrawalRequestDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.auth.AuthService;
import com.provit.util.jwt.JwtProvider;

/**
 * 인증 및 회원 관리 REST Controller
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final AuthCookieService authCookieService;
    private final RefreshTokenDAO refreshTokenDAO;

    @Autowired
    public AuthController(AuthService authService, JwtProvider jwtProvider, AuthCookieService authCookieService,
            RefreshTokenDAO refreshTokenDAO) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
        this.authCookieService = authCookieService;
        this.refreshTokenDAO = refreshTokenDAO;
    }

    /**
     * 1. 이메일 중복 확인
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam("email") String email) {
        boolean available = authService.isEmailAvailable(email);
        ResponseCode code = available ? ResponseCode.AUTH_EMAIL_AVAILABLE : ResponseCode.AUTH_EMAIL_DUPLICATE;
        ApiResponse<Map<String, Boolean>> response = new ApiResponse<>(code, Collections.singletonMap("available", available));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authCookieService.getRefreshToken(request).ifPresent(this::deleteRefreshToken);
        return new ResponseEntity<>(new ApiResponse<>(ResponseCode.SUCCESS_EMPTY, null), clearAuthCookies(), HttpStatus.OK);
    }

    /**
     * 2. 닉네임 중복 확인
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam("nickname") String nickname) {
        boolean available = authService.isNicknameAvailable(nickname);
        ResponseCode code = available ? ResponseCode.AUTH_NICKNAME_AVAILABLE : ResponseCode.AUTH_NICKNAME_DUPLICATE;
        ApiResponse<Map<String, Boolean>> response = new ApiResponse<>(code, Collections.singletonMap("available", available));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 3. 이메일 인증번호 발송
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sendVerificationCode(@RequestBody EmailSendRequestDTO requestDTO) {
        long expiresAt = authService.sendVerificationEmail(requestDTO.getEmail());
        // 서버에서 계산한 만료 시각을 내려줘 클라이언트 타이머 오차를 줄인다.
        ApiResponse<Map<String, Long>> response = new ApiResponse<>(ResponseCode.AUTH_CODE_SENT, Collections.singletonMap("expiresAt", expiresAt));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 4. 이메일 인증번호 확인 (검증 완료 토큰 반환)
     */
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyCode(@RequestBody EmailVerifyRequestDTO requestDTO) {
        String verificationToken = authService.verifyEmailCode(requestDTO.getEmail(), requestDTO.getCode());
        ApiResponse<Map<String, String>> response = new ApiResponse<>(ResponseCode.AUTH_CODE_VERIFIED, Collections.singletonMap("verificationToken", verificationToken));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/password-reset/send-code")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sendPasswordResetVerificationCode(
            @RequestBody EmailSendRequestDTO requestDTO) {
        long expiresAt = authService.sendPasswordResetVerificationEmail(requestDTO.getEmail());
        ApiResponse<Map<String, Long>> response = new ApiResponse<>(ResponseCode.AUTH_CODE_SENT,
                Collections.singletonMap("expiresAt", expiresAt));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/password-reset/verify-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPasswordResetCode(
            @RequestBody EmailVerifyRequestDTO requestDTO) {
        String verificationToken = authService.verifyPasswordResetCode(requestDTO.getEmail(), requestDTO.getCode());
        ApiResponse<Map<String, String>> response = new ApiResponse<>(ResponseCode.AUTH_CODE_VERIFIED,
                Collections.singletonMap("verificationToken", verificationToken));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody PasswordResetRequestDTO requestDTO) {
        authService.resetPassword(requestDTO);
        return new ResponseEntity<>(new ApiResponse<>(ResponseCode.SUCCESS_EMPTY, null), HttpStatus.OK);
    }

    /**
     * 5. 신규 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDTO>> signup(@RequestBody SignupRequestDTO requestDTO) {
        UserResponseDTO userResponse = authService.signup(requestDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(ResponseCode.AUTH_SIGNUP_SUCCESS, userResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 6. 로그인 (JWT 발급)
     */
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO loginResponse = authService.login(requestDTO);
        refreshTokenDAO.deleteExpired();
        saveRefreshToken(loginResponse.getRefreshToken());
        ApiResponse<LoginResponseDTO> response = new ApiResponse<>(ResponseCode.AUTH_LOGIN_SUCCESS, loginResponse);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService
                .createAccessTokenCookie(loginResponse.getAccessToken(), jwtProvider.getExpirationTime())
                .toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieService
                .createRefreshTokenCookie(loginResponse.getRefreshToken(), jwtProvider.getRefreshExpirationTime())
                .toString());
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        String refreshToken = authCookieService.getRefreshToken(request).orElse(null);
        if (refreshToken == null || !jwtProvider.validateRefreshToken(refreshToken)) {
            return unauthorizedRefreshResponse();
        }

        String tokenId = jwtProvider.getTokenId(refreshToken);
        RefreshTokenDTO storedToken = refreshTokenDAO.selectByTokenId(tokenId);
        Long userNum = jwtProvider.getUserNum(refreshToken);
        if (storedToken == null || !userNum.equals(storedToken.getUserNum())
                || storedToken.getExpiresAt() == null || !storedToken.getExpiresAt().after(new Date())) {
            refreshTokenDAO.deleteByTokenId(tokenId);
            return unauthorizedRefreshResponse();
        }

        refreshTokenDAO.deleteExpired();
        refreshTokenDAO.deleteByTokenId(tokenId);
        String newAccessToken = jwtProvider.createToken(userNum);
        String newRefreshToken = jwtProvider.createRefreshToken(userNum);
        saveRefreshToken(newRefreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService
                .createAccessTokenCookie(newAccessToken, jwtProvider.getExpirationTime()).toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieService
                .createRefreshTokenCookie(newRefreshToken, jwtProvider.getRefreshExpirationTime()).toString());
        return new ResponseEntity<>(new ApiResponse<>(ResponseCode.SUCCESS_EMPTY, null), headers, HttpStatus.OK);
    }

    /**
     * 7. 현재 로그인 유저 정보 조회 (토큰 검증 및 세션 복원)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyProfile(HttpServletRequest request) {
        String token = authCookieService.getAccessToken(request).orElse(null);
        if (token == null) return unauthorizedProfileResponse(null);
        if (!jwtProvider.validateToken(token)) {
            return unauthorizedProfileResponse(token);
        }

        Long userNum = jwtProvider.getUserNum(token);
        UserResponseDTO userProfile = authService.getUserProfile(userNum);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(ResponseCode.SUCCESS, userProfile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // JWT로 식별한 현재 로그인 회원만 자신의 닉네임/비밀번호를 수정한다.
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyProfile(
            HttpServletRequest request,
            @RequestBody MyPageUpdateRequestDTO requestDTO) {
        String token = authCookieService.getAccessToken(request).orElse(null);
        if (token == null) return unauthorizedProfileResponse(null);
        if (!jwtProvider.validateToken(token)) {
            return unauthorizedProfileResponse(token);
        }

        // 요청 본문의 회원 번호를 신뢰하지 않고 JWT의 회원 번호를 사용한다.
        Long userNum = jwtProvider.getUserNum(token);
        UserResponseDTO updatedProfile = authService.updateMyProfile(userNum, requestDTO);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(ResponseCode.SUCCESS, updatedProfile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // JWT의 회원 번호와 현재 비밀번호를 모두 검증한 뒤 소프트 삭제 처리한다.
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMyAccount(
            HttpServletRequest request,
            @RequestBody WithdrawalRequestDTO requestDTO) {
        String token = authCookieService.getAccessToken(request).orElse(null);
        if (token == null) return unauthorizedResponse(null);
        if (!jwtProvider.validateToken(token)) {
            return unauthorizedResponse(token);
        }

        authService.withdrawMyAccount(jwtProvider.getUserNum(token), requestDTO);
        ApiResponse<Void> response = new ApiResponse<>(ResponseCode.SUCCESS_EMPTY, null);
        return new ResponseEntity<>(response, clearAuthCookies(), HttpStatus.OK);
    }

    private void saveRefreshToken(String refreshToken) {
        refreshTokenDAO.insert(RefreshTokenDTO.builder()
                .tokenId(jwtProvider.getTokenId(refreshToken))
                .userNum(jwtProvider.getUserNum(refreshToken))
                .expiresAt(jwtProvider.getExpiration(refreshToken))
                .build());
    }

    private void deleteRefreshToken(String refreshToken) {
        try {
            refreshTokenDAO.deleteByTokenId(jwtProvider.getTokenId(refreshToken));
        } catch (RuntimeException ignored) {
            // 만료되었거나 형식이 잘못된 쿠키는 만료 정리 시 삭제됩니다.
        }
        refreshTokenDAO.deleteExpired();
    }

    private HttpHeaders clearAuthCookies() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearAccessTokenCookie().toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString());
        return headers;
    }

    private ResponseEntity<ApiResponse<Void>> unauthorizedRefreshResponse() {
        return new ResponseEntity<>(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null), clearAuthCookies(),
                HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<ApiResponse<UserResponseDTO>> unauthorizedProfileResponse(String token) {
        ResponseCode code = token == null ? ResponseCode.AUTH_UNAUTHORIZED
                : jwtProvider.isTokenExpired(token) ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearAccessTokenCookie().toString());
        return new ResponseEntity<>(new ApiResponse<>(code, null), headers, HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<ApiResponse<Void>> unauthorizedResponse(String token) {
        ResponseCode code = token == null ? ResponseCode.AUTH_UNAUTHORIZED
                : jwtProvider.isTokenExpired(token) ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieService.clearAccessTokenCookie().toString());
        return new ResponseEntity<>(new ApiResponse<>(code, null), headers, HttpStatus.UNAUTHORIZED);
    }
}
