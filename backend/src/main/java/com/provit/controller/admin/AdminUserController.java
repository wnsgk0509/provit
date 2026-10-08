package com.provit.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.admin.AdminUserDTO;
import com.provit.dto.admin.AdminUserRequestDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.dto.common.PageResponseDTO;
import com.provit.service.admin.AdminUserService;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final com.provit.dao.admin.AdminReportDAO adminReportDAO;

    @Autowired
    public AdminUserController(AdminUserService adminUserService, com.provit.dao.admin.AdminReportDAO adminReportDAO) {
        this.adminUserService = adminUserService;
        this.adminReportDAO = adminReportDAO;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<AdminUserDTO>>> getUserList(
            @LoginUser Long loginUserNum,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {

        if (loginUserNum == null || !adminReportDAO.isAdmin(loginUserNum)) {
            throw new SecurityException("관리자 권한이 없습니다.");
        }

        PageResponseDTO<AdminUserDTO> response = adminUserService.getUserList(page, size, keyword, status);
        return ResponseEntity.ok(new ApiResponse<>(ResponseCode.SUCCESS, response));
    }

    @PutMapping("/{userNum}/status")
    public ResponseEntity<ApiResponse<String>> updateUserBlockStatus(
            @LoginUser Long loginUserNum,
            @PathVariable("userNum") Long userNum,
            @RequestBody AdminUserRequestDTO requestDTO) {

        if (loginUserNum == null || !adminReportDAO.isAdmin(loginUserNum)) {
            throw new SecurityException("관리자 권한이 없습니다.");
        }

        adminUserService.updateUserBlockStatus(userNum, requestDTO);
        return ResponseEntity.ok(new ApiResponse<>(ResponseCode.SUCCESS, "계정 상태가 변경되었습니다."));
    }
}
