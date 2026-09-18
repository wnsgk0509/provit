package com.provit.dto.user;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CoverLetterDTO {

    private int userNum;
    private String growthProcess;
    private String personalityStrengthsWeaknesses;
    private String problemSolvingExperience;
    private String postJoiningAspiration;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date updatedAt;
}
