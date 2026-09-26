package com.provit.service.interview.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
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
import com.provit.dto.interview.LlmQuestionResponseDTO;
import com.provit.service.interview.InterviewService;
import com.provit.service.interview.InterviewProcessingException;
import com.provit.service.interview.InterviewPersistenceService;
import com.provit.service.interview.InterviewDocumentInputBuilder;
import com.provit.service.interview.generator.InterviewGenerator;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);
    private static final int ANSWER_TIME_LIMIT_SECONDS = 120;
    private static final int MAX_GENERATOR_CALLS = 4;
    private static final long SESSION_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final InterviewDAO interviewDAO;
    private final InterviewGenerator interviewGenerator;
    private final InterviewPersistenceService persistenceService;
    private final InterviewDocumentInputBuilder documentInputBuilder;
    private final Map<Integer, InterviewSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, StartAttempt> starts = new ConcurrentHashMap<>();

    @Autowired
    public InterviewServiceImpl(
            InterviewDAO interviewDAO,
            InterviewGenerator interviewGenerator,
            InterviewPersistenceService persistenceService,
            InterviewDocumentInputBuilder documentInputBuilder) {
        this.interviewDAO = interviewDAO;
        this.interviewGenerator = interviewGenerator;
        this.persistenceService = persistenceService;
        this.documentInputBuilder = documentInputBuilder;
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
        validateDocumentNumbers(resumeNum, portfolioNum, letterNum);

        LlmInterviewContextDTO context = new LlmInterviewContextDTO();
        context.setJobPreference(interviewDAO.selectUserJobPreferenceByUserNum(userNum));
        context.setResumeDetail(getResumeDetail(userNum, resumeNum));
        if (portfolioNum > 0) {
            context.setPortfolio(interviewDAO.selectPortfolioByPortfolioNumAndUserNum(portfolioNum, userNum));
        }
        context.setCoverLetter(interviewDAO.selectCoverLetterByLetterNumAndUserNum(letterNum, userNum));
        validateSelectedDocuments(context, portfolioNum);
        documentInputBuilder.prepare(context);

        return context;
    }

    @Override
    public InterviewStartResponseDTO startInterview(int userNum, InterviewStartRequestDTO request) {
        validateStartRequest(request);
        if (request.getRequestId() == null) return createSession(userNum, request);
        if (!request.getRequestId().matches("[a-fA-F0-9-]{36}")) {
            throw new IllegalArgumentException("면접 시작 요청 번호가 올바르지 않습니다.");
        }
        String key = userNum + ":" + request.getRequestId();
        StartAttempt attempt = starts.computeIfAbsent(key, ignored -> new StartAttempt(request));
        synchronized (attempt) {
            if (!attempt.fingerprint.equals(startFingerprint(request))) {
                throw new IllegalArgumentException("같은 요청 번호로 면접 설정을 변경할 수 없습니다.");
            }
            if (attempt.response != null) {
                InterviewSession session = sessions.get(attempt.response.getHistoryNum());
                if (session == null || session.isInactive(System.currentTimeMillis())) {
                    throw new InterviewProcessingException("면접이 만료되었습니다. 새 면접을 시작해 주세요.", true, false);
                }
                return attempt.response;
            }
            if (attempt.failure != null) throw attempt.failure;
            try {
                attempt.response = createSession(userNum, request);
                return attempt.response;
            } catch (RuntimeException exception) {
                attempt.failure = exception;
                throw exception;
            }
        }
    }

    private InterviewStartResponseDTO createSession(int userNum, InterviewStartRequestDTO request) {
        LlmInterviewContextDTO context = getLlmInterviewContext(
                userNum, request.getResumeNum(), request.getPortfolioNum(), request.getLetterNum());

        LlmQuestionRequestDTO questionRequest = new LlmQuestionRequestDTO();
        questionRequest.setContext(context);
        questionRequest.setInterviewStyle(request.getInterviewStyle());
        questionRequest.setInterviewDifficulty(request.getInterviewDifficulty());
        questionRequest.setQuestionAnswers(Collections.emptyList());

        LlmQuestionResponseDTO generated = interviewGenerator.generateDocumentQuestions(questionRequest);
        List<InterviewQuestionDTO> documentQuestions = validateDocumentQuestions(generated);
        int historyNum = interviewDAO.selectNextHistoryNum();
        InterviewSession session = new InterviewSession(userNum, request, context);
        session.questions.addAll(documentQuestions);
        sessions.put(historyNum, session);

        InterviewStartResponseDTO response = new InterviewStartResponseDTO();
        response.setHistoryNum(historyNum);
        response.setQuestions(List.of(documentQuestions.get(0)));
        response.setAnswerTimeLimitSeconds(ANSWER_TIME_LIMIT_SECONDS);
        return response;
    }

    @Override
    public InterviewAnswerResponseDTO submitAnswer(
            int userNum, int historyNum, InterviewAnswerRequestDTO request) {
        InterviewSession session = sessions.get(historyNum);
        if (session == null || session.userNum != userNum) {
            throw new InterviewProcessingException("진행 중인 면접을 찾을 수 없습니다. 새 면접을 시작해 주세요.", true, false);
        }

        synchronized (session) {
            if (session.isInactive(System.currentTimeMillis())) {
                sessions.remove(historyNum, session);
                throw new InterviewProcessingException("진행 중인 면접이 만료되었습니다. 새 면접을 시작해 주세요.", true, false);
            }
            session.touch();

            if (request != null && request.getQuestionOrder() > 0
                    && request.getQuestionOrder() <= session.questionAnswers.size()) {
                int answeredIndex = request.getQuestionOrder() - 1;
                String submittedAnswer = request.getAnswer() == null ? "" : request.getAnswer();
                if (!session.questionAnswers.get(answeredIndex).getAnswer().equals(submittedAnswer)) {
                    throw new IllegalArgumentException("이미 제출한 답변은 변경할 수 없습니다.");
                }
                return session.responses.get(answeredIndex);
            }
            if (session.failed) {
                throw new InterviewProcessingException("면접 처리에 실패했습니다. 새 면접을 시작해 주세요.", true, false);
            }

            int expectedOrder = session.questionAnswers.size() + 1;
            if (request != null) {
                request.setTimedOut(session.hasAnswerExpired());
            }
            validateAnswerRequest(request, expectedOrder);

            InterviewQuestionDTO currentQuestion = session.questions.get(expectedOrder - 1);
            InterviewQuestionAnswerDTO currentAnswer = createQuestionAnswer(currentQuestion, request);
            if (session.evaluatedAnswer != null && !session.evaluatedAnswer.getAnswer().equals(currentAnswer.getAnswer())) {
                throw new InterviewProcessingException("평가가 완료된 답변은 변경할 수 없습니다. 기존 답변으로 다시 저장해 주세요.", false, true);
            }

            InterviewAnswerResponseDTO response = new InterviewAnswerResponseDTO();
            if (expectedOrder < 5) {
                InterviewQuestionDTO nextQuestion;
                if (expectedOrder < 3) {
                    nextQuestion = session.questions.get(expectedOrder);
                } else {
                    reserveGeneratorCall(session);
                    try {
                        nextQuestion = generateFollowUpQuestion(session, expectedOrder + 1, currentAnswer);
                        validateFollowUpQuestion(nextQuestion, expectedOrder + 1);
                    } catch (RuntimeException exception) {
                        session.failed = true;
                        throw generationFailure(exception);
                    }
                    session.questions.add(nextQuestion);
                }
                session.questionAnswers.add(currentAnswer);
                session.restartAnswerTimer();
                response.setCompleted(false);
                response.setNextQuestion(nextQuestion);
                session.responses.add(response);
                return response;
            }

            if (session.evaluation == null) {
                reserveGeneratorCall(session);
                try {
                    session.evaluation = evaluate(session, userNum, currentAnswer);
                    session.evaluatedAnswer = currentAnswer;
                } catch (RuntimeException exception) {
                    session.failed = true;
                    throw generationFailure(exception);
                }
            }
            InterviewHistoryDTO history = createHistory(historyNum, userNum, session);
            setHistoryQuestionAnswer(history, session.evaluatedAnswer);
            InterviewResultDTO result = createResult(historyNum, userNum, session.evaluation);
            try {
                saveInterview(history, result);
            } catch (RuntimeException exception) {
                log.warn("Interview history={} save failed type={}", historyNum, exception.getClass().getSimpleName());
                throw new InterviewProcessingException("평가는 완료됐지만 결과를 저장하지 못했습니다. 같은 답변으로 다시 제출해 주세요.", false, true);
            }

            session.questionAnswers.add(session.evaluatedAnswer);
            response.setCompleted(true);
            response.setResult(createResultResponse(historyNum, session.evaluation));
            session.responses.add(response);
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

    @Scheduled(fixedDelay = 300000)
    public void removeInactiveSessions() {
        long now = System.currentTimeMillis();
        int removedCount = 0;

        for (Map.Entry<Integer, InterviewSession> entry : sessions.entrySet()) {
            InterviewSession session = entry.getValue();
            synchronized (session) {
                if (session.isInactive(now) && sessions.remove(entry.getKey(), session)) {
                    removedCount++;
                }
            }
        }

        if (removedCount > 0) {
            log.info("30분 이상 미응답인 면접 세션 {}건을 정리했습니다.", removedCount);
        }
        for (var entry : starts.entrySet()) {
            StartAttempt attempt = entry.getValue();
            synchronized (attempt) {
                if (now - attempt.createdAt >= SESSION_TIMEOUT_MILLIS
                        && (attempt.response == null || !sessions.containsKey(attempt.response.getHistoryNum()))) {
                    starts.remove(entry.getKey(), attempt);
                }
            }
        }
    }

    private InterviewProcessingException generationFailure(RuntimeException exception) {
        String message = exception instanceof InterviewProcessingException
                ? exception.getMessage() : "AI 면접 처리에 실패했습니다. 새 면접을 시작해 주세요.";
        return new InterviewProcessingException(message, true, false);
    }

    private static String startFingerprint(InterviewStartRequestDTO request) {
        return request.getResumeNum() + ":" + request.getPortfolioNum() + ":" + request.getLetterNum()
                + ":" + request.getInterviewStyle() + ":" + request.getInterviewDifficulty();
    }

    private static class StartAttempt {
        private final long createdAt = System.currentTimeMillis();
        private final String fingerprint;
        private InterviewStartResponseDTO response;
        private RuntimeException failure;
        private StartAttempt(InterviewStartRequestDTO request) { fingerprint = startFingerprint(request); }
    }

    private InterviewQuestionDTO generateFollowUpQuestion(
            InterviewSession session, int questionOrder, InterviewQuestionAnswerDTO currentAnswer) {
        LlmFollowUpRequestDTO request = new LlmFollowUpRequestDTO();
        request.setContext(session.context);
        request.setInterviewStyle(session.settings.getInterviewStyle());
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(answersIncluding(session, currentAnswer));
        return interviewGenerator.generateFollowUpQuestion(questionOrder, request);
    }

    private LlmEvaluationResponseDTO evaluate(
            InterviewSession session, int userNum, InterviewQuestionAnswerDTO currentAnswer) {
        LlmEvaluationRequestDTO request = new LlmEvaluationRequestDTO();
        request.setContext(session.context);
        request.setInterviewStyle(session.settings.getInterviewStyle());
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(answersIncluding(session, currentAnswer));
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

    private List<InterviewQuestionAnswerDTO> answersIncluding(
            InterviewSession session, InterviewQuestionAnswerDTO currentAnswer) {
        List<InterviewQuestionAnswerDTO> answers = new ArrayList<>(session.questionAnswers);
        answers.add(currentAnswer);
        return List.copyOf(answers);
    }

    private void reserveGeneratorCall(InterviewSession session) {
        if (session.generatorCalls >= MAX_GENERATOR_CALLS) {
            throw new IllegalStateException("면접의 질문 생성 호출 한도를 초과했습니다.");
        }
        session.generatorCalls++;
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
        if (request == null) {
            throw new IllegalArgumentException("면접 시작 정보를 입력해 주세요.");
        }
        validateDocumentNumbers(
                request.getResumeNum(), request.getPortfolioNum(), request.getLetterNum());
        if (request.getInterviewStyle() == null || request.getInterviewStyle().isBlank()
                || request.getInterviewDifficulty() == null || request.getInterviewDifficulty().isBlank()) {
            throw new IllegalArgumentException("면접 방식과 난이도를 선택해 주세요.");
        }
        if (!List.of("RANDOM", "ONE_TO_ONE", "PANEL", "GROUP").contains(request.getInterviewStyle())
                || !List.of("EASY", "NORMAL", "HARD").contains(request.getInterviewDifficulty())) {
            throw new IllegalArgumentException("면접 방식 또는 난이도가 올바르지 않습니다.");
        }
    }

    private void validateDocumentNumbers(int resumeNum, int portfolioNum, int letterNum) {
        if (resumeNum <= 0) {
            throw new IllegalArgumentException("이력서를 선택해 주세요.");
        }
        if (letterNum <= 0) {
            throw new IllegalArgumentException("자기소개서를 선택해 주세요.");
        }
    }

    private void validateSelectedDocuments(LlmInterviewContextDTO context, int portfolioNum) {
        if (portfolioNum > 0 && context.getPortfolio() == null) {
            throw new IllegalArgumentException("선택한 포트폴리오를 찾을 수 없습니다.");
        }
        if (context.getCoverLetter() == null) {
            throw new IllegalArgumentException("선택한 자기소개서를 찾을 수 없습니다.");
        }
    }

    private List<InterviewQuestionDTO> validateDocumentQuestions(LlmQuestionResponseDTO response) {
        if (response == null || response.getQuestions() == null || response.getQuestions().size() != 3) {
            throw new IllegalStateException("서류 질문은 정확히 3개여야 합니다.");
        }
        List<InterviewQuestionDTO> questions = response.getQuestions();
        for (int index = 0; index < questions.size(); index++) {
            validateQuestion(questions.get(index), index + 1, "DOCUMENT");
        }
        return List.copyOf(questions);
    }

    private void validateFollowUpQuestion(InterviewQuestionDTO question, int order) {
        validateQuestion(question, order, "FOLLOW_UP");
    }

    private void validateQuestion(InterviewQuestionDTO question, int order, String type) {
        if (question == null || question.getQuestionOrder() != order
                || !type.equals(question.getQuestionType())
                || question.getQuestionText() == null || question.getQuestionText().isBlank()
                || question.getQuestionText().length() > 1000) {
            throw new IllegalStateException("면접 질문 형식이 올바르지 않습니다.");
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
        private final List<InterviewAnswerResponseDTO> responses = new ArrayList<>();
        private LlmEvaluationResponseDTO evaluation;
        private InterviewQuestionAnswerDTO evaluatedAnswer;
        private int generatorCalls = 1;
        private boolean failed;
        private volatile long lastActivityAtMillis;
        private long questionStartedAtNanos;

        private InterviewSession(
                int userNum, InterviewStartRequestDTO settings, LlmInterviewContextDTO context) {
            this.userNum = userNum;
            this.settings = settings;
            this.context = context;
            this.lastActivityAtMillis = System.currentTimeMillis();
            this.questionStartedAtNanos = System.nanoTime();
        }

        private void touch() {
            lastActivityAtMillis = System.currentTimeMillis();
        }

        private boolean isInactive(long now) {
            return now - lastActivityAtMillis >= SESSION_TIMEOUT_MILLIS;
        }

        private boolean hasAnswerExpired() {
            return System.nanoTime() - questionStartedAtNanos
                    >= TimeUnit.SECONDS.toNanos(ANSWER_TIME_LIMIT_SECONDS);
        }

        private void restartAnswerTimer() {
            questionStartedAtNanos = System.nanoTime();
        }
    }

}
