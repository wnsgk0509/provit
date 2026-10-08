package com.provit.service.admin.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.admin.AdminUserDAO;
import com.provit.dto.admin.AdminUserDTO;
import com.provit.dto.admin.AdminUserRequestDTO;
import com.provit.dto.common.PageResponseDTO;
import com.provit.service.admin.AdminUserService;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserDAO adminUserDAO;

    @Autowired
    public AdminUserServiceImpl(AdminUserDAO adminUserDAO) {
        this.adminUserDAO = adminUserDAO;
    }

    @Override
    public PageResponseDTO<AdminUserDTO> getUserList(int page, int size, String keyword, String status) {
        int offset = (page - 1) * size;
        
        Map<String, Object> params = new HashMap<>();
        params.put("offset", offset);
        params.put("limit", size);
        params.put("keyword", keyword);
        params.put("status", status); // 'ACTIVE', 'BLOCKED'

        List<AdminUserDTO> items = adminUserDAO.selectUserList(params);
        int totalCount = adminUserDAO.selectUserCount(params);

        return new PageResponseDTO<>(items, totalCount, page, size);
    }

    @Override
    @Transactional
    public void updateUserBlockStatus(Long userNum, AdminUserRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getBlockDays() == null) {
            throw new IllegalArgumentException("정지 기간을 설정해주세요.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("userNum", userNum);
        params.put("blockDays", requestDTO.getBlockDays());

        adminUserDAO.updateBlockedDate(params);
    }
}
