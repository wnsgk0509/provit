package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewStartRequestDTO {

    private boolean usePortfolio;
    private boolean useCoverLetter;
    private String interviewStyle;
    private String interviewDifficulty;
}
