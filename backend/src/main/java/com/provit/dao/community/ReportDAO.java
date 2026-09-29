package com.provit.dao.community;

import com.provit.dto.community.ReportDTO;

public interface ReportDAO {
    int checkDuplicateReport(ReportDTO reportDTO) throws Exception;
    void insertReport(ReportDTO reportDTO) throws Exception;
}
