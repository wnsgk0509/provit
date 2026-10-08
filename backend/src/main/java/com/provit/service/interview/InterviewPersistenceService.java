package com.provit.service.interview;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;

@Service
public class InterviewPersistenceService {

    private final InterviewDAO interviewDAO;

    public InterviewPersistenceService(InterviewDAO interviewDAO) {
        this.interviewDAO = interviewDAO;
    }

    @Transactional
    public int save(InterviewHistoryDTO history, InterviewResultDTO result) {
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
}
