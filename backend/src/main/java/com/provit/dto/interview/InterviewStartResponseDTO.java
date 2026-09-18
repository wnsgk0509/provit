package com.provit.dto.interview;

import java.util.List;

import lombok.Data;

@Data
public class InterviewStartResponseDTO {

    private Long historyNum;
    private List<InterviewQuestionDTO> questions;
    private int answerTimeLimitSeconds;
}
