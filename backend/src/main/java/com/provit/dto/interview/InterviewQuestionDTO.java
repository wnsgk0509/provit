package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewQuestionDTO {

    private Integer questionOrder;
    private String questionType;
    private String questionText;
}
