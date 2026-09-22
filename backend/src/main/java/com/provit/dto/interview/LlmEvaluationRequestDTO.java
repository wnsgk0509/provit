package com.provit.dto.interview;

import java.util.List;

import lombok.Data;

@Data
public class LlmEvaluationRequestDTO {

    private LlmInterviewContextDTO context;
    private String interviewStyle;
    private String interviewDifficulty;
    private List<InterviewQuestionAnswerDTO> questionAnswers;
    private InterviewResultDTO previousResult;
}
