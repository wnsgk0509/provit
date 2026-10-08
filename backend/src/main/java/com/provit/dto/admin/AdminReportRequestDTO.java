package com.provit.dto.admin;

import lombok.Data;

@Data
public class AdminReportRequestDTO {
    private String reportStatus; // RESOLVED(블라인드), REJECTED(반려)
}
