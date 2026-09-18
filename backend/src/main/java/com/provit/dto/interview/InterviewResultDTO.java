package com.provit.dto.interview;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class InterviewResultDTO {

    private Long historyNum;
    private Integer userNum;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date interviewDate;
}
