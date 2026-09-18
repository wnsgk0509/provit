package com.provit.dto.user;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CertificationDTO {

    private int certNum;
    private int resumeNum;
    private String certName;
    private String certGrade;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date issueDate;
}
