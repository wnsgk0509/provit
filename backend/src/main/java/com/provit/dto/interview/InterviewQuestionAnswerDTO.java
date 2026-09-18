package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewQuestionAnswerDTO {

    private Integer questionOrder;
    private String questionType;
    private String question;
    private String answer;
    private boolean timedOut;
}
