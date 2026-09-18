package com.provit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 회원가입 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"userPw", "confirmPw"})
public class SignupRequestDto {

    private String userName;
    private String userNickname;
    private String userEmail;
    private String userPw;
    private String confirmPw;
    private String userBirthDate; // YYYY-MM-DD
    private String jobCode;
    private String occupationCode;
    private String verificationToken;
}
