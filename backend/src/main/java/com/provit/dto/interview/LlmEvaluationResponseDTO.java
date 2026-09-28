package com.provit.dto.interview;

import lombok.Data;

@Data
public class LlmEvaluationResponseDTO {

    private double documentConsistencyScore;
    private double expertiseScore;
    private double problemSolvingScore;
    private double logicScore;
    private double communicationScore;
    private double totalScore;
    private String strengths;
    private String weaknesses;
    private String comparison;
    private String improvements;
}
