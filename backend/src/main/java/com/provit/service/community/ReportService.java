package com.provit.service.community;

import com.provit.dto.community.ReportDTO;

public interface ReportService {
    void submitReport(ReportDTO reportDTO) throws Exception;
}
