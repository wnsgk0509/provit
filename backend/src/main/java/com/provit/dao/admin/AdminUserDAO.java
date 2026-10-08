package com.provit.dao.admin;

import java.util.List;
import java.util.Map;

import com.provit.dto.admin.AdminUserDTO;

public interface AdminUserDAO {
    List<AdminUserDTO> selectUserList(Map<String, Object> params);
    int selectUserCount(Map<String, Object> params);
    void updateBlockedDate(Map<String, Object> params);
}
