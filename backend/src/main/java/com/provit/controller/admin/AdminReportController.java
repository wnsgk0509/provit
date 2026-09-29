package com.provit.controller.admin;

import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.admin.AdminReportDTO;
import com.provit.dto.admin.AdminReportRequestDTO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.admin.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Autowired
    private AdminReportService adminReportService;

    // 1. 신고 목록 조회 (페이징, 상태별 필터)
    @GetMapping
    public ApiResponse<PageResponseDTO<AdminReportDTO>> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @LoginUser Long userNum) {
        
        // TODO: 실제 어드민 권한 체크 로직 추가 필요 (현재는 일단 유저 식별자 존재 여부만 확인)
        if (userNum == null) {
            return ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED);
        }

        PageResponseDTO<AdminReportDTO> response = adminReportService.getReportList(page, size, status);
        return ApiResponse.success(response);
    }

    // 2. 신고 상태 업데이트 (반려 OR 블라인드 처리)
    @PutMapping("/{reportNum}")
    public ApiResponse<?> updateReportStatus(
            @PathVariable Long reportNum,
            @RequestBody AdminReportRequestDTO requestDTO,
            @LoginUser Long userNum) {
        
        if (userNum == null) {
            return ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED);
        }

        try {
            adminReportService.updateReportStatus(reportNum, requestDTO);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(ResponseCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "상태 변경 처리 중 오류가 발생했습니다.");
        }
    }
}
