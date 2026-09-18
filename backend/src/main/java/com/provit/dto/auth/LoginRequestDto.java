package com.provit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 로그인 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "userPw")
public class LoginRequestDto {

    private String userEmail;
    private String userPw;
}
