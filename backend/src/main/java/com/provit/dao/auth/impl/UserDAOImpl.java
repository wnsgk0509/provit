package com.provit.dao.auth.impl;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.UserDTO;

@Repository
public class UserDAOImpl implements UserDAO {

    private static final String NAMESPACE = "com.provit.mapper.UserMapper";

    private final SqlSession sqlSession;

    @Autowired
    public UserDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public UserDTO selectByEmail(String userEmail) {
        return sqlSession.selectOne(NAMESPACE + ".selectByEmail", userEmail);
    }

    @Override
    public UserDTO selectByNickname(String userNickname) {
        return sqlSession.selectOne(NAMESPACE + ".selectByNickname", userNickname);
    }

    @Override
    public UserDTO selectByUserNum(Long userNum) {
        return sqlSession.selectOne(NAMESPACE + ".selectByUserNum", userNum);
    }

    @Override
    public int countByEmail(String userEmail) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countByEmail", userEmail);
        return count != null ? count : 0;
    }

    @Override
    public int countByNickname(String userNickname) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countByNickname", userNickname);
        return count != null ? count : 0;
    }

    @Override
    public int insertUser(UserDTO userDTO) {
        return sqlSession.insert(NAMESPACE + ".insertUser", userDTO);
    }

    @Override
    public int updateMyProfile(UserDTO userDTO) {
        return sqlSession.update(NAMESPACE + ".updateMyProfile", userDTO);
    }

    @Override
    public int withdrawMyAccount(Long userNum) {
        return sqlSession.update(NAMESPACE + ".withdrawMyAccount", userNum);
    }
}
