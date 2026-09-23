package com.provit.dto.recruitment;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RecruitmentDTO {

    private Long recruitmentNum;
    private String saraminJobId;
    private String companyName;
    private String title;
    private String jobUrl;
    private String locationName;
    private String jobName;
    private String experienceLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date expirationDate;

    private String closeType;
    private Integer isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date createdAt;

    // 현재 요청 사용자의 스크랩(북마크) 여부 (로그인 사용자 기준, 비로그인은 false)
    @Builder.Default
    private Boolean isScrapped = false;
}
