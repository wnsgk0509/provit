package com.provit.dto.admin;

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
public class AdminUserRequestDTO {
    // 0: 해제, 7: 7일 정지, 30: 30일 정지, 9999: 무기한 정지 (2099년)
    private Integer blockDays; 
}
