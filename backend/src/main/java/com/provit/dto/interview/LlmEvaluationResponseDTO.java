package com.provit.dto.interview;

import lombok.Data;

@Data
public class LlmEvaluationResponseDTO {

    private double confidenceScore;
    private double persistenceScore;
    private double expertiseScore;
    private double logicScore;
    private double deliveryScore;
    private double totalScore;
    private String strengths;
    private String weaknesses;
    private String comparison;
    private String improvements;
}
