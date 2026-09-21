package com.provit.dto.auth;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "userPw")
public class UserDTO {

    private Long userNum;
    private String userName;
    private String userNickname;
    private Date userBirthDate;
    private String userEmail;
    private String userPw;
    private Date userRegisterDate;
    private String userType;
    private String jobCode;
    private String occupationCode;
    private Integer userIsDeleted;
    private Integer userTokenVersion;
}
