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
@ToString(exclude = {"currentPassword", "newPassword", "newPasswordConfirm"})
public class MyPageUpdateRequestDTO {

    // null이면 해당 항목을 수정하지 않는다.
    private String userNickname;
    // 비밀번호 변경 요청일 때만 세 필드를 함께 전달한다.
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
}
