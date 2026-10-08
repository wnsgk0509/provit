package com.provit.service.auth;

import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.MyPageUpdateRequestDTO;
import com.provit.dto.auth.PasswordResetRequestDTO;
import com.provit.dto.auth.SignupRequestDTO;
import com.provit.dto.auth.UserResponseDTO;
import com.provit.dto.auth.WithdrawalRequestDTO;

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
    long sendVerificationEmail(String email);

    /**
     * 이메일 인증번호 검증 (성공 시 가입용 검증 토큰 반환)
     */
    String verifyEmailCode(String email, String code);

    /**
     * 가입된 계정의 비밀번호 재설정용 인증번호를 발송한다.
     */
    long sendPasswordResetVerificationEmail(String email);

    /**
     * 비밀번호 재설정용 인증번호를 검증하고 재설정 토큰을 발급한다.
     */
    String verifyPasswordResetCode(String email, String code);

    /**
     * 이메일 인증이 완료된 계정의 비밀번호를 변경한다.
     */
    void resetPassword(PasswordResetRequestDTO requestDTO);

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

    /**
     * 토큰으로 식별한 회원의 닉네임 및 비밀번호를 수정한다.
     */
    UserResponseDTO updateMyProfile(Long userNum, MyPageUpdateRequestDTO requestDTO);

    /**
     * 회원 데이터를 물리 삭제하지 않고 탈퇴 상태로 변경한다.
     */
    void withdrawMyAccount(Long userNum, WithdrawalRequestDTO requestDTO);
}
