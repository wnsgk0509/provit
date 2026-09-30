package com.provit.dao.admin;

import com.provit.dto.admin.AdminReportDTO;
import java.util.List;
import java.util.Map;

public interface AdminReportDAO {
    
    // 전체 신고 목록 조회 (검색 및 페이징 가능하도록 파라미터 맵 활용)
    List<AdminReportDTO> selectReportList(Map<String, Object> params);
    
    // 관리자 여부 확인
    boolean isAdmin(Long userNum);
    
    // 전체 신고 건수
    int selectReportCount(Map<String, Object> params);

    // 단일 신고 내역 조회
    AdminReportDTO selectReportDetail(Long reportNum);
    
    // 신고 상태 변경
    int updateReportStatus(Map<String, Object> params);
    
    // 게시글 블라인드 처리 (제목, 내용 변경)
    int blindPost(Long postNum);
    
    // 댓글 블라인드 처리 (내용 변경 및 삭제상태 변경)
    int blindComment(Long commentNum);
}
