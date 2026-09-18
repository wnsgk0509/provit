package com.provit.dao.auth;

import com.provit.dto.auth.UserDTO;

/**
 * T_USER 테이블 접근 DAO 인터페이스
 */
public interface UserDAO {

    UserDTO selectByEmail(String userEmail);

    UserDTO selectByNickname(String userNickname);

    UserDTO selectByUserNum(Long userNum);

    int countByEmail(String userEmail);

    int countByNickname(String userNickname);

    int insertUser(UserDTO userDTO);
}
