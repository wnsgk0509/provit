package com.provit.service.auth;

import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.SignupRequestDTO;
import com.provit.dto.auth.UserResponseDTO;

/**
 * 인증 및 회원 관리 비즈니스 로직 인터페이스
 */
public interface AuthService {

    /**
     * 이메일 중복 확인 (true: 사용 가능, false: 중복)
     */
    boolean isEmailAvailable(String email);

    /**
     * 닉네임 중복 확인 (true: 사용 가능, false: 중복)
     */
    boolean isNicknameAvailable(String nickname);

    /**
     * 이메일 인증번호 생성 및 발송
     */
    void sendVerificationEmail(String email);

    /**
     * 이메일 인증번호 검증 (성공 시 가입용 검증 토큰 반환)
     */
    String verifyEmailCode(String email, String code);

    /**
     * 신규 회원가입 처리
     */
    UserResponseDTO signup(SignupRequestDTO requestDTO);

    /**
     * 로그인 처리 및 JWT 발급
     */
    LoginResponseDTO login(LoginRequestDTO requestDTO);

    /**
     * 토큰 기반 회원 프로필 조회
     */
    UserResponseDTO getUserProfile(Long userNum);
}
