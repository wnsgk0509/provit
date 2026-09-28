package com.provit.dao.interview.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewDocumentOptionDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

@Repository
public class InterviewDAOImpl implements InterviewDAO {

    private static final String PORTFOLIO_NAMESPACE = "com.provit.mapper.document.PortfolioMapper";
    private static final String COVER_LETTER_NAMESPACE = "com.provit.mapper.document.CoverLetterMapper";
    private static final String RESUME_NAMESPACE = "com.provit.mapper.document.ResumeMapper";
    private static final String USER_JOB_NAMESPACE = "com.provit.mapper.interview.UserJobMapper";
    private static final String INTERVIEW_HISTORY_NAMESPACE = "com.provit.mapper.interview.InterviewHistoryMapper";
    private static final String INTERVIEW_RESULT_NAMESPACE = "com.provit.mapper.interview.InterviewResultMapper";

    private final SqlSession sqlSession;

    @Autowired
    public InterviewDAOImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public List<InterviewDocumentOptionDTO> selectPortfolioListByUserNum(int userNum) {
        return sqlSession.selectList(PORTFOLIO_NAMESPACE + ".selectPortfolioListByUserNum", userNum);
    }

    @Override
    public PortfolioDTO selectPortfolioByPortfolioNumAndUserNum(int portfolioNum, int userNum) {
        return sqlSession.selectOne(
                PORTFOLIO_NAMESPACE + ".selectPortfolio",
                createDocumentKeyMap("portfolioNum", portfolioNum, userNum));
    }

    @Override
    public List<InterviewDocumentOptionDTO> selectCoverLetterListByUserNum(int userNum) {
        return sqlSession.selectList(COVER_LETTER_NAMESPACE + ".selectCoverLetterListByUserNum", userNum);
    }

    @Override
    public CoverLetterDTO selectCoverLetterByLetterNumAndUserNum(int letterNum, int userNum) {
        return sqlSession.selectOne(
                COVER_LETTER_NAMESPACE + ".selectCoverLetter",
                createDocumentKeyMap("letterNum", letterNum, userNum));
    }

    @Override
    public List<InterviewDocumentOptionDTO> selectResumeListByUserNum(int userNum) {
        return sqlSession.selectList(RESUME_NAMESPACE + ".selectResumeListByUserNum", userNum);
    }

    @Override
    public ResumeDTO selectResumeByResumeNumAndUserNum(int resumeNum, int userNum) {
        return sqlSession.selectOne(
                RESUME_NAMESPACE + ".selectResume",
                createDocumentKeyMap("resumeNum", resumeNum, userNum));
    }

    @Override
    public List<EducationDTO> selectEducationListByResumeNum(int resumeNum) {
        return sqlSession.selectList(RESUME_NAMESPACE + ".selectEducationList", resumeNum);
    }

    @Override
    public List<CareerDTO> selectCareerListByResumeNum(int resumeNum) {
        return sqlSession.selectList(RESUME_NAMESPACE + ".selectCareerList", resumeNum);
    }

    @Override
    public List<CertificationDTO> selectCertificationListByResumeNum(int resumeNum) {
        return sqlSession.selectList(RESUME_NAMESPACE + ".selectCertificationList", resumeNum);
    }

    @Override
    public UserJobPreferenceDTO selectUserJobPreferenceByUserNum(int userNum) {
        return sqlSession.selectOne(USER_JOB_NAMESPACE + ".selectUserJobPreferenceByUserNum", userNum);
    }

    @Override
    public int selectNextHistoryNum() {
        return sqlSession.selectOne(INTERVIEW_HISTORY_NAMESPACE + ".selectNextHistoryNum");
    }

    @Override
    public int insertInterviewHistory(InterviewHistoryDTO history) {
        return sqlSession.insert(INTERVIEW_HISTORY_NAMESPACE + ".insertInterviewHistory", history);
    }

    @Override
    public int insertInterviewResult(InterviewResultDTO result) {
        return sqlSession.insert(INTERVIEW_RESULT_NAMESPACE + ".insertInterviewResult", result);
    }

    @Override
    public List<InterviewResultDTO> selectInterviewResultListByUserNum(int userNum) {
        return sqlSession.selectList(INTERVIEW_RESULT_NAMESPACE + ".selectInterviewResultListByUserNum", userNum);
    }

    @Override
    public InterviewHistoryDTO selectInterviewHistory(int historyNum, int userNum) {
        return sqlSession.selectOne(
                INTERVIEW_HISTORY_NAMESPACE + ".selectInterviewHistory",
                createInterviewKeyMap(historyNum, userNum));
    }

    @Override
    public InterviewResultDTO selectInterviewResult(int historyNum, int userNum) {
        return sqlSession.selectOne(
                INTERVIEW_RESULT_NAMESPACE + ".selectInterviewResult",
                createInterviewKeyMap(historyNum, userNum));
    }

    @Override
    public InterviewResultDTO selectLatestInterviewResultByUserNum(int userNum) {
        return sqlSession.selectOne(
                INTERVIEW_RESULT_NAMESPACE + ".selectLatestInterviewResultByUserNum",
                userNum);
    }

    private Map<String, Object> createInterviewKeyMap(int number, int userNum) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("number", number);
        parameters.put("userNum", userNum);
        return parameters;
    }

    private Map<String, Object> createDocumentKeyMap(String documentKey, int number, int userNum) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(documentKey, number);
        parameters.put("userNum", userNum);
        return parameters;
    }
}
