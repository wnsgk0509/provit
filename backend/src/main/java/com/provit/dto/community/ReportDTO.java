package com.provit.dto.community;

import lombok.Data;
import java.util.Date;

@Data
public class ReportDTO {
    private Long reportNum;
    private Long reporterNum;
    private String targetType; // 'POST' or 'COMMENT'
    private Long targetNum;
    private String reportReason;
    private String reportStatus; // 'PENDING', 'BLIND', 'REJECT'
    private Date reportDate;
}
