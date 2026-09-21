package com.provit.dto.interview;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class InterviewHistoryDTO {

    private int historyNum;
    private int userNum;
    private String question1;
    private String answer1;
    private String question2;
    private String answer2;
    private String question3;
    private String answer3;
    private String question4;
    private String answer4;
    private String question5;
    private String answer5;
    private String strength;
    private String weakness;
    private String previousComparison;
    private String improvementPoint;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date interviewDate;
}
