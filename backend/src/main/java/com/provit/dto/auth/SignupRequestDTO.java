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
    private String jobCode;
    private String occupationCode;
    private String verificationToken;
}
