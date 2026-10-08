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
@ToString(exclude = {"verificationToken", "newPassword", "confirmPassword"})
public class PasswordResetRequestDTO {

    private String verificationToken;
    private String newPassword;
    private String confirmPassword;
}
