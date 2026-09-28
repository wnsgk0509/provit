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
@ToString(exclude = {"userPw", "confirmPw"})
public class SignupRequestDTO {

    private String userName;
    private String userNickname;
    private String userEmail;
    private String userPw;
    private String confirmPw;
    private String userBirthDate;
    // 기존 회원가입 화면은 두 코드를 보내지 않으므로 선택값으로 유지한다.
    private String occupationCode;
    private String jobCode;
    private String verificationToken;
}
