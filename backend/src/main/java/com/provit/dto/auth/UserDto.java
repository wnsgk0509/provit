package com.provit.dto.auth;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * T_USER 테이블과 매핑되는 회원 엔티티 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "userPw")
public class UserDto {

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
}
