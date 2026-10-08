package com.provit.dao.community.impl;

import com.provit.dao.community.ReportDAO;
import com.provit.dto.community.ReportDTO;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReportDAOImpl implements ReportDAO {
    
    @Autowired
    private SqlSession sqlSession;
    
    private static final String NAMESPACE = "com.provit.mapper.ReportMapper";

    @Override
    public int checkDuplicateReport(ReportDTO reportDTO) throws Exception {
        return sqlSession.selectOne(NAMESPACE + ".checkDuplicateReport", reportDTO);
    }

    @Override
    public void insertReport(ReportDTO reportDTO) throws Exception {
        sqlSession.insert(NAMESPACE + ".insertReport", reportDTO);
    }
}
