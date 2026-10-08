package com.provit.dao.admin.impl;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.admin.AdminUserDAO;
import com.provit.dto.admin.AdminUserDTO;

@Repository
public class AdminUserDAOImpl implements AdminUserDAO {

    private final SqlSession sqlSession;
    private static final String NAMESPACE = "com.provit.mapper.AdminUserMapper";

    @Autowired
    public AdminUserDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public List<AdminUserDTO> selectUserList(Map<String, Object> params) {
        return sqlSession.selectList(NAMESPACE + ".selectUserList", params);
    }

    @Override
    public int selectUserCount(Map<String, Object> params) {
        return sqlSession.selectOne(NAMESPACE + ".selectUserCount", params);
    }

    @Override
    public void updateBlockedDate(Map<String, Object> params) {
        sqlSession.update(NAMESPACE + ".updateBlockedDate", params);
    }
}
