package com.provit.dto.admin;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDTO {
    private Long userNum;
    private String userEmail;
    private String userName;
    private String userNickname;
    private String userType;
    private Date userRegisterDate;
    private Integer userIsDeleted;
    private Date blockedDate;
}
