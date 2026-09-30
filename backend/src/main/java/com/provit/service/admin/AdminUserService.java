package com.provit.service.admin;

import com.provit.dto.admin.AdminUserRequestDTO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.dto.admin.AdminUserDTO;

public interface AdminUserService {
    PageResponseDTO<AdminUserDTO> getUserList(int page, int size, String keyword, String status);
    void updateUserBlockStatus(Long userNum, AdminUserRequestDTO requestDTO);
}
