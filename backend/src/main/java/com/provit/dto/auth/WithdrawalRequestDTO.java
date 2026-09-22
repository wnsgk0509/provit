package com.provit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "currentPassword")
public class WithdrawalRequestDTO {

    // 탈퇴 요청자가 현재 로그인 회원인지 한 번 더 검증하는 비밀번호다.
    private String currentPassword;
}
