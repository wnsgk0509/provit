package com.provit.dao.interview;

import java.util.List;

import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewDocumentOptionDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

public interface InterviewDAO {

    List<InterviewDocumentOptionDTO> selectPortfolioListByUserNum(int userNum);

    PortfolioDTO selectPortfolioByPortfolioNumAndUserNum(int portfolioNum, int userNum);

    List<InterviewDocumentOptionDTO> selectCoverLetterListByUserNum(int userNum);

    CoverLetterDTO selectCoverLetterByLetterNumAndUserNum(int letterNum, int userNum);

    List<InterviewDocumentOptionDTO> selectResumeListByUserNum(int userNum);

    ResumeDTO selectResumeByResumeNumAndUserNum(int resumeNum, int userNum);

    List<EducationDTO> selectEducationListByResumeNum(int resumeNum);

    List<CareerDTO> selectCareerListByResumeNum(int resumeNum);

    List<CertificationDTO> selectCertificationListByResumeNum(int resumeNum);

    UserJobPreferenceDTO selectUserJobPreferenceByUserNum(int userNum);

    int selectNextHistoryNum();

    int insertInterviewHistory(InterviewHistoryDTO history);

    int insertInterviewResult(InterviewResultDTO result);

    List<InterviewResultDTO> selectInterviewResultListByUserNum(int userNum);

    InterviewHistoryDTO selectInterviewHistory(int historyNum, int userNum);

    InterviewResultDTO selectInterviewResult(int historyNum, int userNum);

    InterviewResultDTO selectLatestInterviewResultByUserNum(int userNum);
}
