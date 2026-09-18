package com.provit.dto.interview;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class InterviewResultResponseDTO {

    private Long historyNum;
    private BigDecimal confidenceScore;
    private BigDecimal persistenceScore;
    private BigDecimal expertiseScore;
    private BigDecimal logicScore;
    private BigDecimal deliveryScore;
    private BigDecimal totalScore;
    private String strengths;
    private String weaknesses;
    private String comparison;
    private String improvements;
    private String overallFeedback;
}
