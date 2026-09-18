package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewAnswerRequestDTO {

    private Integer questionOrder;
    private String answer;
    private boolean timedOut;
}
