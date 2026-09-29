package com.provit.service.admin;

import com.provit.dto.admin.AdminReportDTO;
import com.provit.dto.admin.AdminReportRequestDTO;
import com.provit.dto.common.PageResponseDTO;

public interface AdminReportService {
    
    // 관리자 여부 확인
    boolean isAdmin(Long userNum);
    
    // 전체 신고 목록 조회 (페이징, 상태 필터링)
    PageResponseDTO<AdminReportDTO> getReportList(int page, int size, String status);
    
    // 신고 상태 변경 및 블라인드 처리
    void updateReportStatus(Long reportNum, AdminReportRequestDTO requestDTO) throws Exception;
}
