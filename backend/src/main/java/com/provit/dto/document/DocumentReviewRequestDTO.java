package com.provit.dto.document;

import lombok.Data;

@Data
public class DocumentReviewRequestDTO {
    private String requestId;
    private Integer resumeNum;
    private Integer letterNum;
    private Integer portfolioNum;
    private String reviewMode;
    private String customCriteria;
    private String instructions;
}
