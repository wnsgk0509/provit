package com.provit.dto.interview;

import lombok.Data;

@Data
public class LlmQuestionRequestDTO {

    private LlmInterviewContextDTO context;
    private String interviewStyle;
    private String interviewDifficulty;
}
