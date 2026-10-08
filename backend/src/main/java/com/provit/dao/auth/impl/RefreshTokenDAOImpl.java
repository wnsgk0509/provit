package com.provit.dao.auth.impl;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.provit.dao.auth.RefreshTokenDAO;
import com.provit.dto.auth.RefreshTokenDTO;

@Repository
public class RefreshTokenDAOImpl implements RefreshTokenDAO {

    private static final String NAMESPACE = "com.provit.mapper.RefreshTokenMapper";

    private final SqlSession sqlSession;

    public RefreshTokenDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public int insert(RefreshTokenDTO refreshToken) {
        return sqlSession.insert(NAMESPACE + ".insert", refreshToken);
    }

    @Override
    public RefreshTokenDTO selectByTokenId(String tokenId) {
        return sqlSession.selectOne(NAMESPACE + ".selectByTokenId", tokenId);
    }

    @Override
    public int deleteByTokenId(String tokenId) {
        return sqlSession.delete(NAMESPACE + ".deleteByTokenId", tokenId);
    }

    @Override
    public int deleteExpired() {
        return sqlSession.delete(NAMESPACE + ".deleteExpired");
    }
}
