package com.provit.dto.auth;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프론트엔드 반환용 회원 응답 DTO (비밀번호 및 해시값 완전 배제)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long userNum;
    private String userName;
    private String userNickname;
    private Date userBirthDate;
    private String userEmail;
    private Date userRegisterDate;
    private String userType;
    private String jobCode;
    private String occupationCode;

    public static UserResponseDto from(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        return UserResponseDto.builder()
                .userNum(userDto.getUserNum())
                .userName(userDto.getUserName())
                .userNickname(userDto.getUserNickname())
                .userBirthDate(userDto.getUserBirthDate())
                .userEmail(userDto.getUserEmail())
                .userRegisterDate(userDto.getUserRegisterDate())
                .userType(userDto.getUserType())
                .jobCode(userDto.getJobCode())
                .occupationCode(userDto.getOccupationCode())
                .build();
    }
}
