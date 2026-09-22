package com.provit.dao.document.impl;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.user.CareerDTO;
import com.provit.dto.user.CertificationDTO;
import com.provit.dto.user.EducationDTO;
import com.provit.dto.user.ResumeDTO;

@Repository
public class DocumentDAOImpl implements DocumentDAO {

    private static final String NAMESPACE = "com.provit.mapper.document.ResumeWriteMapper";

    private final SqlSession sqlSession;

    @Autowired
    public DocumentDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public int countEducationCode(int educationCode) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countEducationCode", educationCode);
        return count == null ? 0 : count;
    }

    @Override
    public int insertResume(ResumeDTO resume) {
        return sqlSession.insert(NAMESPACE + ".insertResume", resume);
    }

    @Override
    public int insertEducation(EducationDTO education) {
        return sqlSession.insert(NAMESPACE + ".insertEducation", education);
    }

    @Override
    public int insertCareer(CareerDTO career) {
        return sqlSession.insert(NAMESPACE + ".insertCareer", career);
    }

    @Override
    public int insertCertification(CertificationDTO certification) {
        return sqlSession.insert(NAMESPACE + ".insertCertification", certification);
    }
}
