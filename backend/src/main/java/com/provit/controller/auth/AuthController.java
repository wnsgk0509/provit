package com.provit.controller.auth;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.auth.EmailSendRequestDTO;
import com.provit.dto.auth.EmailVerifyRequestDTO;
import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.MyPageUpdateRequestDTO;
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

    @Autowired
    public AuthController(AuthService authService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
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
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(@RequestBody EmailSendRequestDTO requestDTO) {
        authService.sendVerificationEmail(requestDTO.getEmail());
        ApiResponse<String> response = new ApiResponse<>(ResponseCode.AUTH_CODE_SENT, "인증번호가 이메일로 발송되었습니다.");
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
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO loginResponse = authService.login(requestDTO);
        ApiResponse<LoginResponseDTO> response = new ApiResponse<>(ResponseCode.AUTH_LOGIN_SUCCESS, loginResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 7. 현재 로그인 유저 정보 조회 (토큰 검증 및 세션 복원)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyProfile(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            ResponseCode code = jwtProvider.isTokenExpired(token) ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(code, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            ResponseCode code = jwtProvider.isTokenExpired(token) ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
            ApiResponse<UserResponseDTO> response = new ApiResponse<>(code, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<Void> response = new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            ResponseCode code = jwtProvider.isTokenExpired(token) ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
            ApiResponse<Void> response = new ApiResponse<>(code, null);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        authService.withdrawMyAccount(jwtProvider.getUserNum(token), requestDTO);
        ApiResponse<Void> response = new ApiResponse<>(ResponseCode.SUCCESS_EMPTY, null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
