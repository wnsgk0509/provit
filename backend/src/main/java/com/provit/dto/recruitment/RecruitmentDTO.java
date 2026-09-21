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
}
