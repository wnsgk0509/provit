package com.provit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 이메일 인증번호 검증 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "code")
public class EmailVerifyRequestDto {

    private String email;
    private String code;
}
