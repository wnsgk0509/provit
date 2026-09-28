package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewAnswerResponseDTO {

    private boolean completed;
    private InterviewQuestionDTO nextQuestion;
    private InterviewResultResponseDTO result;
    private long questionDeadline;
    private long expiresAt;
    private long serverTime;
}
