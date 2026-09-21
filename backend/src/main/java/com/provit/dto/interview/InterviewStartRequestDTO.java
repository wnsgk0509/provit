package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewStartRequestDTO {

    private int resumeNum;
    private int portfolioNum;
    private int letterNum;
    private String interviewStyle;
    private String interviewDifficulty;
}
