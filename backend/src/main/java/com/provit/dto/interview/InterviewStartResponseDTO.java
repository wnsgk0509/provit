package com.provit.dto.interview;

import java.util.List;

import lombok.Data;

@Data
public class InterviewStartResponseDTO {

    private int historyNum;
    private List<InterviewQuestionDTO> questions;
    private int answerTimeLimitSeconds;
}
