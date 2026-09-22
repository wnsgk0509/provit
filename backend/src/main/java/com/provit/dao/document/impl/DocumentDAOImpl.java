package com.provit.dao.document.impl;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.user.CareerDTO;
import com.provit.dto.user.CertificationDTO;
import com.provit.dto.user.EducationDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDTO;

@Repository
public class DocumentDAOImpl implements DocumentDAO {

    private static final String RESUME_NAMESPACE = "com.provit.mapper.document.ResumeWriteMapper";
    private static final String PORTFOLIO_NAMESPACE = "com.provit.mapper.document.PortfolioWriteMapper";

    private final SqlSession sqlSession;

    @Autowired
    public DocumentDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public int countEducationCode(int educationCode) {
        Integer count = sqlSession.selectOne(RESUME_NAMESPACE + ".countEducationCode", educationCode);
        return count == null ? 0 : count;
    }

    @Override
    public int insertResume(ResumeDTO resume) {
        return sqlSession.insert(RESUME_NAMESPACE + ".insertResume", resume);
    }

    @Override
    public int insertEducation(EducationDTO education) {
        return sqlSession.insert(RESUME_NAMESPACE + ".insertEducation", education);
    }

    @Override
    public int insertCareer(CareerDTO career) {
        return sqlSession.insert(RESUME_NAMESPACE + ".insertCareer", career);
    }

    @Override
    public int insertCertification(CertificationDTO certification) {
        return sqlSession.insert(RESUME_NAMESPACE + ".insertCertification", certification);
    }

    @Override
    public int selectNextPortfolioNum() {
        Integer portfolioNum = sqlSession.selectOne(PORTFOLIO_NAMESPACE + ".selectNextPortfolioNum");
        if (portfolioNum == null) {
            throw new IllegalStateException("포트폴리오 번호를 발급하지 못했습니다.");
        }
        return portfolioNum;
    }

    @Override
    public int insertPortfolio(PortfolioDTO portfolio) {
        return sqlSession.insert(PORTFOLIO_NAMESPACE + ".insertPortfolio", portfolio);
    }
}
