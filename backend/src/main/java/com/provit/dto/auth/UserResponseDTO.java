package com.provit.dto.auth;

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
public class UserResponseDTO {

    private Long userNum;
    private String userName;
    private String userNickname;
    private Date userBirthDate;
    private String userEmail;
    private Date userRegisterDate;
    private String userType;
    private String jobCode;
    private String occupationCode;

    public static UserResponseDTO from(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        return UserResponseDTO.builder()
                .userNum(userDTO.getUserNum())
                .userName(userDTO.getUserName())
                .userNickname(userDTO.getUserNickname())
                .userBirthDate(userDTO.getUserBirthDate())
                .userEmail(userDTO.getUserEmail())
                .userRegisterDate(userDTO.getUserRegisterDate())
                .userType(userDTO.getUserType())
                .jobCode(userDTO.getJobCode())
                .occupationCode(userDTO.getOccupationCode())
                .build();
    }
}
