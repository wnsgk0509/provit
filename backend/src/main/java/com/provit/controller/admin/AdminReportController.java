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

    @GetMapping
    public ApiResponse<?> getReportList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @LoginUser Long userNum) {
        
        try {
            if (userNum == null || !adminReportService.isAdmin(userNum)) {
                return ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED, "관리자 권한이 없습니다.");
            }

            PageResponseDTO<AdminReportDTO> response = adminReportService.getReportList(page, size, status);
            return ApiResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 목록 조회 중 오류가 발생했습니다.");
        }
    }

    // 2. 신고 상태 업데이트 (반려 OR 블라인드 처리)
    @PutMapping("/{reportNum}")
    public ApiResponse<?> updateReportStatus(
            @PathVariable("reportNum") Long reportNum,
            @RequestBody AdminReportRequestDTO requestDTO,
            @LoginUser Long userNum) {
        
        if (userNum == null || !adminReportService.isAdmin(userNum)) {
            return ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED, "관리자 권한이 없습니다.");
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
