package com.provit.dao.admin.impl;

import com.provit.dao.admin.AdminReportDAO;
import com.provit.dto.admin.AdminReportDTO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AdminReportDAOImpl implements AdminReportDAO {

    @Autowired
    private SqlSession sqlSession;
    
    private static final String NAMESPACE = "com.provit.mapper.AdminReportMapper";

    @Override
    public List<AdminReportDTO> selectReportList(Map<String, Object> params) {
        return sqlSession.selectList(NAMESPACE + ".selectReportList", params);
    }
    
    @Override
    public boolean isAdmin(Long userNum) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".isAdmin", userNum);
        return count != null && count > 0;
    }

    @Override
    public int selectReportCount(Map<String, Object> params) {
        return sqlSession.selectOne(NAMESPACE + ".selectReportCount", params);
    }

    @Override
    public AdminReportDTO selectReportDetail(Long reportNum) {
        return sqlSession.selectOne(NAMESPACE + ".selectReportDetail", reportNum);
    }

    @Override
    public int updateReportStatus(Map<String, Object> params) {
        return sqlSession.update(NAMESPACE + ".updateReportStatus", params);
    }

    @Override
    public int blindPost(Long postNum) {
        return sqlSession.update(NAMESPACE + ".blindPost", postNum);
    }

    @Override
    public int blindComment(Long commentNum) {
        return sqlSession.update(NAMESPACE + ".blindComment", commentNum);
    }
}
