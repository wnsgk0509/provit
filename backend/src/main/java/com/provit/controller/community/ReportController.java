package com.provit.controller.community;

import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.community.ReportDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.community.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ApiResponse<?> submitReport(
            @RequestBody ReportDTO reportDTO,
            @LoginUser Long userNum) {
        try {
            if (userNum == null) {
                return ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED);
            }
            
            reportDTO.setReporterNum(userNum);
            reportService.submitReport(reportDTO);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(ResponseCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 처리 중 오류가 발생했습니다.");
        }
    }
}
