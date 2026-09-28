package com.provit.dto.interview;

import java.util.List;
import lombok.Data;

@Data
public class InterviewSessionResponseDTO {
    private int historyNum;
    private InterviewStartRequestDTO settings;
    private List<InterviewQuestionDTO> questions;
    private List<InterviewQuestionAnswerDTO> answers;
    private boolean completed;
    private boolean answerLocked;
    private String pendingAnswer;
    private InterviewResultResponseDTO result;
    private int answerTimeLimitSeconds;
    private long questionDeadline;
    private long expiresAt;
    private long serverTime;
}
