package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewAnswerRequestDTO {

    private int questionOrder;
    private String answer;
    private boolean timedOut;
}
