package com.provit.dto.interview;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class InterviewResultDTO {

    private int historyNum;
    private int userNum;
    private double confidenceScore;
    private double persistenceScore;
    private double expertiseScore;
    private double logicScore;
    private double deliveryScore;
    private double totalScore;
    private String strengths;
    private String weaknesses;
    private String comparison;
    private String improvements;
    private String overallFeedback;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date interviewDate;
}
