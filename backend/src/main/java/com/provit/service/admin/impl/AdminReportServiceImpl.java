package com.provit.service.admin.impl;

import com.provit.dao.admin.AdminReportDAO;
import com.provit.dto.admin.AdminReportDTO;
import com.provit.dto.admin.AdminReportRequestDTO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.service.admin.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportServiceImpl implements AdminReportService {

    @Autowired
    private AdminReportDAO adminReportDAO;

    @Override
    public boolean isAdmin(Long userNum) {
        return adminReportDAO.isAdmin(userNum);
    }

    @Override
    public PageResponseDTO<AdminReportDTO> getReportList(int page, int size, String status) {
        int offset = (page - 1) * size;
        
        Map<String, Object> params = new HashMap<>();
        params.put("offset", offset);
        params.put("limit", size);
        params.put("status", status); // 'PENDING', 'RESOLVED', 'REJECTED' or null for all

        List<AdminReportDTO> list = adminReportDAO.selectReportList(params);
        int totalElements = adminReportDAO.selectReportCount(params);
        
        return new PageResponseDTO<>(list, totalElements, page, size);
    }

    @Override
    @Transactional
    public void updateReportStatus(Long reportNum, AdminReportRequestDTO requestDTO) throws Exception {
        // 1. 신고 내역 조회
        AdminReportDTO report = adminReportDAO.selectReportDetail(reportNum);
        if (report == null) {
            throw new IllegalArgumentException("존재하지 않는 신고 번호입니다.");
        }
        
        String newStatus = requestDTO.getReportStatus();
        
        // 2. 상태 업데이트
        Map<String, Object> params = new HashMap<>();
        params.put("reportNum", reportNum);
        params.put("status", newStatus);
        
        int result = adminReportDAO.updateReportStatus(params);
        if (result == 0) {
            throw new Exception("신고 상태 변경에 실패했습니다.");
        }
        
        // 3. 'RESOLVED' (블라인드 처리)일 경우 해당 원본 데이터 내용 변경
        if ("RESOLVED".equals(newStatus)) {
            if ("POST".equals(report.getTargetType())) {
                adminReportDAO.blindPost(report.getTargetNum());
            } else if ("COMMENT".equals(report.getTargetType())) {
                adminReportDAO.blindComment(report.getTargetNum());
            }
        }
    }
}
