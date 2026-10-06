package com.provit.dao.document.impl;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.provit.dao.document.MainResumeDAO;
import com.provit.dto.document.MainResumeJobInfoDTO;

@Repository
public class MainResumeDAOImpl implements MainResumeDAO {
    private static final String NAMESPACE = "com.provit.mapper.document.MainResumeMapper";
    private final SqlSession sqlSession;

    public MainResumeDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public int updateMainResume(long userNum, long resumeNum) {
        return sqlSession.update(NAMESPACE + ".updateMainResume",
                Map.of("userNum", userNum, "resumeNum", resumeNum));
    }

    @Override
    public int clearMainResume(long userNum) {
        return sqlSession.update(NAMESPACE + ".clearMainResume", userNum);
    }

    @Override
    public MainResumeJobInfoDTO selectMainResumeJobInfo(long userNum) {
        return sqlSession.selectOne(NAMESPACE + ".selectMainResumeJobInfo", userNum);
    }
}
