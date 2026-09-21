package com.provit.service.interview.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.interview.InterviewAnswerRequestDTO;
import com.provit.dto.interview.InterviewAnswerResponseDTO;
import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewQuestionAnswerDTO;
import com.provit.dto.interview.InterviewQuestionDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.InterviewResultResponseDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.InterviewStartResponseDTO;
import com.provit.dto.interview.LlmEvaluationRequestDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;
import com.provit.dto.interview.LlmFollowUpRequestDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;
import com.provit.dto.user.ResumeDTO;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.service.interview.InterviewService;
import com.provit.service.interview.InterviewPersistenceService;
import com.provit.service.interview.generator.InterviewGenerator;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final InterviewDAO interviewDAO;
    private final InterviewGenerator interviewGenerator;
    private final InterviewPersistenceService persistenceService;
    private final Map<Integer, InterviewSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public InterviewServiceImpl(
            InterviewDAO interviewDAO,
            InterviewGenerator interviewGenerator,
            InterviewPersistenceService persistenceService) {
        this.interviewDAO = interviewDAO;
        this.interviewGenerator = interviewGenerator;
        this.persistenceService = persistenceService;
    }

    @Override
    public InterviewDocumentResponseDTO getInterviewDocuments(int userNum) {
        InterviewDocumentResponseDTO response = new InterviewDocumentResponseDTO();
        response.setPortfolioList(interviewDAO.selectPortfolioListByUserNum(userNum));
        response.setCoverLetterList(interviewDAO.selectCoverLetterListByUserNum(userNum));
        response.setResumeList(interviewDAO.selectResumeListByUserNum(userNum));
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
            int portfolioNum,
            int letterNum) {
        LlmInterviewContextDTO context = new LlmInterviewContextDTO();
        context.setJobPreference(interviewDAO.selectUserJobPreferenceByUserNum(userNum));
        context.setResumeDetail(getResumeDetail(userNum, resumeNum));

        if (portfolioNum > 0) {
            context.setPortfolio(interviewDAO.selectPortfolioByPortfolioNumAndUserNum(portfolioNum, userNum));
        }
        if (letterNum > 0) {
            context.setCoverLetter(interviewDAO.selectCoverLetterByLetterNumAndUserNum(letterNum, userNum));
        }

        return context;
    }

    @Override
    public InterviewStartResponseDTO startInterview(int userNum, InterviewStartRequestDTO request) {
        validateStartRequest(request);
        LlmInterviewContextDTO context = getLlmInterviewContext(
                userNum, request.getResumeNum(), request.getPortfolioNum(), request.getLetterNum());
        validateSelectedDocuments(request, context);

        LlmQuestionRequestDTO questionRequest = new LlmQuestionRequestDTO();
        questionRequest.setContext(context);
        questionRequest.setInterviewStyle(request.getInterviewStyle());
        questionRequest.setInterviewDifficulty(request.getInterviewDifficulty());
        questionRequest.setQuestionAnswers(Collections.emptyList());

        InterviewQuestionDTO firstQuestion = interviewGenerator.generateDocumentQuestion(1, questionRequest);
        int historyNum = interviewDAO.selectNextHistoryNum();
        InterviewSession session = new InterviewSession(userNum, request, context);
        session.questions.add(firstQuestion);
        sessions.put(historyNum, session);

        InterviewStartResponseDTO response = new InterviewStartResponseDTO();
        response.setHistoryNum(historyNum);
        response.setQuestions(List.of(firstQuestion));
        response.setAnswerTimeLimitSeconds(120);
        return response;
    }

    @Override
    public InterviewAnswerResponseDTO submitAnswer(
            int userNum, int historyNum, InterviewAnswerRequestDTO request) {
        InterviewSession session = sessions.get(historyNum);
        if (session == null || session.userNum != userNum) {
            throw new IllegalArgumentException("진행 중인 면접을 찾을 수 없습니다.");
        }

        synchronized (session) {
            int expectedOrder = session.questionAnswers.size() + 1;
            validateAnswerRequest(request, expectedOrder);

            InterviewQuestionDTO currentQuestion = session.questions.get(expectedOrder - 1);
            session.questionAnswers.add(createQuestionAnswer(currentQuestion, request));

            InterviewAnswerResponseDTO response = new InterviewAnswerResponseDTO();
            if (expectedOrder < 5) {
                InterviewQuestionDTO nextQuestion = generateNextQuestion(session, expectedOrder + 1);
                session.questions.add(nextQuestion);
                response.setCompleted(false);
                response.setNextQuestion(nextQuestion);
                return response;
            }

            LlmEvaluationResponseDTO evaluation = evaluate(session, userNum);
            InterviewHistoryDTO history = createHistory(historyNum, userNum, session);
            InterviewResultDTO result = createResult(historyNum, userNum, evaluation);
            saveInterview(history, result);
            sessions.remove(historyNum);

            response.setCompleted(true);
            response.setResult(createResultResponse(historyNum, evaluation));
            return response;
        }
    }

    @Override
    public int issueHistoryNum() {
        return interviewDAO.selectNextHistoryNum();
    }

    @Override
    public int saveInterview(InterviewHistoryDTO history, InterviewResultDTO result) {
        return persistenceService.save(history, result);
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

    private InterviewQuestionDTO generateNextQuestion(InterviewSession session, int questionOrder) {
        if (questionOrder <= 3) {
            LlmQuestionRequestDTO request = new LlmQuestionRequestDTO();
            request.setContext(session.context);
            request.setInterviewStyle(session.settings.getInterviewStyle());
            request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
            request.setQuestionAnswers(List.copyOf(session.questionAnswers));
            return interviewGenerator.generateDocumentQuestion(questionOrder, request);
        }

        LlmFollowUpRequestDTO request = new LlmFollowUpRequestDTO();
        request.setContext(session.context);
        request.setInterviewStyle(session.settings.getInterviewStyle());
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(List.copyOf(session.questionAnswers));
        return interviewGenerator.generateFollowUpQuestion(questionOrder, request);
    }

    private LlmEvaluationResponseDTO evaluate(InterviewSession session, int userNum) {
        LlmEvaluationRequestDTO request = new LlmEvaluationRequestDTO();
        request.setContext(session.context);
        request.setInterviewStyle(session.settings.getInterviewStyle());
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(List.copyOf(session.questionAnswers));
        request.setPreviousResult(interviewDAO.selectLatestInterviewResultByUserNum(userNum));
        LlmEvaluationResponseDTO evaluation = interviewGenerator.evaluate(request);
        validateEvaluation(evaluation);
        evaluation.setTotalScore(Math.round((
                evaluation.getConfidenceScore()
                + evaluation.getPersistenceScore()
                + evaluation.getExpertiseScore()
                + evaluation.getLogicScore()
                + evaluation.getDeliveryScore()) / 5.0 * 10.0) / 10.0);
        return evaluation;
    }

    private InterviewQuestionAnswerDTO createQuestionAnswer(
            InterviewQuestionDTO question, InterviewAnswerRequestDTO answer) {
        InterviewQuestionAnswerDTO questionAnswer = new InterviewQuestionAnswerDTO();
        questionAnswer.setQuestionOrder(question.getQuestionOrder());
        questionAnswer.setQuestionType(question.getQuestionType());
        questionAnswer.setQuestion(question.getQuestionText());
        questionAnswer.setAnswer(answer.getAnswer() == null ? "" : answer.getAnswer());
        questionAnswer.setTimedOut(answer.isTimedOut());
        return questionAnswer;
    }

    private InterviewHistoryDTO createHistory(
            int historyNum,
            int userNum,
            InterviewSession session) {
        InterviewHistoryDTO history = new InterviewHistoryDTO();
        history.setHistoryNum(historyNum);
        history.setUserNum(userNum);
        for (InterviewQuestionAnswerDTO item : session.questionAnswers) {
            setHistoryQuestionAnswer(history, item);
        }
        return history;
    }

    private void setHistoryQuestionAnswer(InterviewHistoryDTO history, InterviewQuestionAnswerDTO item) {
        switch (item.getQuestionOrder()) {
            case 1 -> { history.setQuestion1(item.getQuestion()); history.setAnswer1(item.getAnswer()); }
            case 2 -> { history.setQuestion2(item.getQuestion()); history.setAnswer2(item.getAnswer()); }
            case 3 -> { history.setQuestion3(item.getQuestion()); history.setAnswer3(item.getAnswer()); }
            case 4 -> { history.setQuestion4(item.getQuestion()); history.setAnswer4(item.getAnswer()); }
            case 5 -> { history.setQuestion5(item.getQuestion()); history.setAnswer5(item.getAnswer()); }
            default -> throw new IllegalArgumentException("질문 순서가 올바르지 않습니다.");
        }
    }

    private InterviewResultDTO createResult(
            int historyNum, int userNum, LlmEvaluationResponseDTO evaluation) {
        InterviewResultDTO result = new InterviewResultDTO();
        result.setHistoryNum(historyNum);
        result.setUserNum(userNum);
        result.setConfidenceScore(evaluation.getConfidenceScore());
        result.setPersistenceScore(evaluation.getPersistenceScore());
        result.setExpertiseScore(evaluation.getExpertiseScore());
        result.setLogicScore(evaluation.getLogicScore());
        result.setDeliveryScore(evaluation.getDeliveryScore());
        result.setTotalScore(evaluation.getTotalScore());
        result.setStrengths(evaluation.getStrengths());
        result.setWeaknesses(evaluation.getWeaknesses());
        result.setComparison(evaluation.getComparison());
        result.setImprovements(evaluation.getImprovements());
        return result;
    }

    private InterviewResultResponseDTO createResultResponse(
            int historyNum, LlmEvaluationResponseDTO evaluation) {
        InterviewResultResponseDTO response = new InterviewResultResponseDTO();
        response.setHistoryNum(historyNum);
        response.setConfidenceScore(evaluation.getConfidenceScore());
        response.setPersistenceScore(evaluation.getPersistenceScore());
        response.setExpertiseScore(evaluation.getExpertiseScore());
        response.setLogicScore(evaluation.getLogicScore());
        response.setDeliveryScore(evaluation.getDeliveryScore());
        response.setTotalScore(evaluation.getTotalScore());
        response.setStrengths(evaluation.getStrengths());
        response.setWeaknesses(evaluation.getWeaknesses());
        response.setComparison(evaluation.getComparison());
        response.setImprovements(evaluation.getImprovements());
        return response;
    }

    private void validateStartRequest(InterviewStartRequestDTO request) {
        if (request == null || request.getResumeNum() <= 0) {
            throw new IllegalArgumentException("이력서를 선택해 주세요.");
        }
        if (request.getInterviewStyle() == null || request.getInterviewStyle().isBlank()
                || request.getInterviewDifficulty() == null || request.getInterviewDifficulty().isBlank()) {
            throw new IllegalArgumentException("면접 방식과 난이도를 선택해 주세요.");
        }
    }

    private void validateSelectedDocuments(
            InterviewStartRequestDTO request, LlmInterviewContextDTO context) {
        if (request.getPortfolioNum() > 0 && context.getPortfolio() == null) {
            throw new IllegalArgumentException("선택한 포트폴리오를 찾을 수 없습니다.");
        }
        if (request.getLetterNum() > 0 && context.getCoverLetter() == null) {
            throw new IllegalArgumentException("선택한 자기소개서를 찾을 수 없습니다.");
        }
    }

    private void validateAnswerRequest(InterviewAnswerRequestDTO request, int expectedOrder) {
        if (request == null || request.getQuestionOrder() != expectedOrder) {
            throw new IllegalArgumentException("답변할 질문 순서가 올바르지 않습니다.");
        }
        String answer = request.getAnswer();
        if (!request.isTimedOut() && (answer == null || answer.isBlank())) {
            throw new IllegalArgumentException("답변을 입력해 주세요.");
        }
        if (answer != null && answer.length() > 1000) {
            throw new IllegalArgumentException("답변은 1000자 이하로 입력해 주세요.");
        }
    }

    private void validateEvaluation(LlmEvaluationResponseDTO evaluation) {
        if (evaluation == null) {
            throw new IllegalStateException("면접 평가 결과가 없습니다.");
        }
        validateScore(evaluation.getConfidenceScore());
        validateScore(evaluation.getPersistenceScore());
        validateScore(evaluation.getExpertiseScore());
        validateScore(evaluation.getLogicScore());
        validateScore(evaluation.getDeliveryScore());
        validateFeedback(evaluation.getStrengths());
        validateFeedback(evaluation.getWeaknesses());
        validateFeedback(evaluation.getComparison());
        validateFeedback(evaluation.getImprovements());
    }

    private void validateScore(double score) {
        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalStateException("면접 평가 점수는 0점 이상 100점 이하여야 합니다.");
        }
    }

    private void validateFeedback(String feedback) {
        if (feedback == null || feedback.isBlank() || feedback.length() > 500) {
            throw new IllegalStateException("면접 평가 문구는 1자 이상 500자 이하여야 합니다.");
        }
    }

    private static class InterviewSession {
        private final int userNum;
        private final InterviewStartRequestDTO settings;
        private final LlmInterviewContextDTO context;
        private final List<InterviewQuestionDTO> questions = new ArrayList<>();
        private final List<InterviewQuestionAnswerDTO> questionAnswers = new ArrayList<>();

        private InterviewSession(
                int userNum, InterviewStartRequestDTO settings, LlmInterviewContextDTO context) {
            this.userNum = userNum;
            this.settings = settings;
            this.context = context;
        }
    }

}
