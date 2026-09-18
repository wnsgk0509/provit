package com.provit.service.interview;

import java.util.List;

import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.dto.user.ResumeDetailDTO;

public interface InterviewService {

    InterviewDocumentResponseDTO getInterviewDocuments(int userNum);

    ResumeDetailDTO getResumeDetail(int userNum, int resumeNum);

    LlmInterviewContextDTO getLlmInterviewContext(
            int userNum,
            int resumeNum,
            boolean usePortfolio,
            boolean useCoverLetter);

    int issueHistoryNum();

    int saveInterview(InterviewHistoryDTO history, InterviewResultDTO result);

    List<InterviewResultDTO> getInterviewResultList(int userNum);

    InterviewHistoryDTO getInterviewHistory(int historyNum, int userNum);

    InterviewResultDTO getInterviewResult(int historyNum, int userNum);

    InterviewResultDTO getLatestInterviewResult(int userNum);
}
