package com.provit.service.interview.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.dto.user.ResumeDTO;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.service.interview.InterviewService;

@Service
@Transactional(readOnly = true)
public class InterviewServiceImpl implements InterviewService {

    private final InterviewDAO interviewDAO;

    @Autowired
    public InterviewServiceImpl(InterviewDAO interviewDAO) {
        this.interviewDAO = interviewDAO;
    }

    @Override
    public InterviewDocumentResponseDTO getInterviewDocuments(int userNum) {
        InterviewDocumentResponseDTO response = new InterviewDocumentResponseDTO();
        response.setPortfolio(interviewDAO.selectPortfolioByUserNum(userNum));
        response.setCoverLetter(interviewDAO.selectCoverLetterByUserNum(userNum));
        response.setResumeList(interviewDAO.selectResumeListByUserNum(userNum));
        response.setJobPreference(interviewDAO.selectUserJobPreferenceByUserNum(userNum));
        return response;
    }

    @Override
    public ResumeDetailDTO getResumeDetail(int userNum, int resumeNum) {
        ResumeDTO resume = interviewDAO.selectResumeByResumeNumAndUserNum(resumeNum, userNum);
        if (resume == null) {
            throw new IllegalArgumentException("선택한 이력서를 찾을 수 없습니다.");
        }

        ResumeDetailDTO detail = new ResumeDetailDTO();
        detail.setResume(resume);
        detail.setEducationList(interviewDAO.selectEducationListByResumeNum(resumeNum));
        detail.setCareerList(interviewDAO.selectCareerListByResumeNum(resumeNum));
        detail.setCertificationList(interviewDAO.selectCertificationListByResumeNum(resumeNum));
        return detail;
    }

    @Override
    public LlmInterviewContextDTO getLlmInterviewContext(
            int userNum,
            int resumeNum,
            boolean usePortfolio,
            boolean useCoverLetter) {
        LlmInterviewContextDTO context = new LlmInterviewContextDTO();
        context.setJobPreference(interviewDAO.selectUserJobPreferenceByUserNum(userNum));
        context.setResumeDetail(getResumeDetail(userNum, resumeNum));

        if (usePortfolio) {
            context.setPortfolio(interviewDAO.selectPortfolioByUserNum(userNum));
        }
        if (useCoverLetter) {
            context.setCoverLetter(interviewDAO.selectCoverLetterByUserNum(userNum));
        }

        return context;
    }

    @Override
    public int issueHistoryNum() {
        return interviewDAO.selectNextHistoryNum();
    }

    @Override
    @Transactional
    public int saveInterview(InterviewHistoryDTO history, InterviewResultDTO result) {
        if (history.getHistoryNum() == 0) {
            history.setHistoryNum(interviewDAO.selectNextHistoryNum());
        }

        result.setHistoryNum(history.getHistoryNum());
        result.setUserNum(history.getUserNum());

        int historyInsertCount = interviewDAO.insertInterviewHistory(history);
        int resultInsertCount = interviewDAO.insertInterviewResult(result);

        if (historyInsertCount != 1 || resultInsertCount != 1) {
            throw new IllegalStateException("면접 결과를 저장하지 못했습니다.");
        }

        return history.getHistoryNum();
    }

    @Override
    public List<InterviewResultDTO> getInterviewResultList(int userNum) {
        return interviewDAO.selectInterviewResultListByUserNum(userNum);
    }

    @Override
    public InterviewHistoryDTO getInterviewHistory(int historyNum, int userNum) {
        return interviewDAO.selectInterviewHistory(historyNum, userNum);
    }

    @Override
    public InterviewResultDTO getInterviewResult(int historyNum, int userNum) {
        return interviewDAO.selectInterviewResult(historyNum, userNum);
    }

    @Override
    public InterviewResultDTO getLatestInterviewResult(int userNum) {
        return interviewDAO.selectLatestInterviewResultByUserNum(userNum);
    }

}
