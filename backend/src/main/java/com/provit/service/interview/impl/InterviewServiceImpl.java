package com.provit.service.interview.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
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
import com.provit.dto.interview.InterviewAvailabilityDTO;
import com.provit.dto.interview.InterviewSessionResponseDTO;
import com.provit.service.interview.InterviewService;
import com.provit.service.interview.InterviewProcessingException;
import com.provit.service.interview.InterviewPersistenceService;
import com.provit.service.interview.InterviewDocumentInputBuilder;
import com.provit.service.interview.InterviewMaintenanceException;
import com.provit.service.interview.generator.InterviewGenerator;
import com.provit.service.interview.InterviewTextLimits;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);
    private static final int ANSWER_TIME_LIMIT_SECONDS = 120;
    private static final int MAX_GENERATOR_CALLS = 4;
    private static final ZoneId INTERVIEW_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime MAINTENANCE_START = LocalTime.of(23, 55);

    private final InterviewDAO interviewDAO;
    private final InterviewGenerator interviewGenerator;
    private final InterviewPersistenceService persistenceService;
    private final InterviewDocumentInputBuilder documentInputBuilder;
    private final Clock clock;
    private final Map<Integer, InterviewSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, StartAttempt> starts = new ConcurrentHashMap<>();

    @Autowired
    public InterviewServiceImpl(
            InterviewDAO interviewDAO,
            InterviewGenerator interviewGenerator,
            InterviewPersistenceService persistenceService,
            InterviewDocumentInputBuilder documentInputBuilder) {
        this(interviewDAO, interviewGenerator, persistenceService, documentInputBuilder, Clock.systemUTC());
    }

    public InterviewServiceImpl(
            InterviewDAO interviewDAO, InterviewGenerator interviewGenerator,
            InterviewPersistenceService persistenceService,
            InterviewDocumentInputBuilder documentInputBuilder, Clock clock) {
        this.interviewDAO = interviewDAO;
        this.interviewGenerator = interviewGenerator;
        this.persistenceService = persistenceService;
        this.documentInputBuilder = documentInputBuilder;
        this.clock = clock;
    }

    @Override
    public InterviewAvailabilityDTO getAvailability() {
        long now = clock.millis();
        var local = Instant.ofEpochMilli(now).atZone(INTERVIEW_ZONE);
        boolean maintenance = !local.toLocalTime().isBefore(MAINTENANCE_START);
        var response = new InterviewAvailabilityDTO();
        response.setMaintenance(maintenance);
        response.setServerTime(now);
        response.setNextChangeAt(maintenance
                ? local.toLocalDate().plusDays(1).atStartOfDay(INTERVIEW_ZONE).toInstant().toEpochMilli()
                : expirationAt(now));
        return response;
    }

    private static long expirationAt(long now) {
        return Instant.ofEpochMilli(now).atZone(INTERVIEW_ZONE).toLocalDate()
                .atTime(MAINTENANCE_START).atZone(INTERVIEW_ZONE).toInstant().toEpochMilli();
    }

    private void ensureAvailable() {
        if (getAvailability().isMaintenance()) throw new InterviewMaintenanceException();
    }

    private void ensureActive(InterviewSession session, int historyNum) {
        ensureAvailable();
        if (session.isExpired(clock.millis()) || sessions.get(historyNum) != session) {
            sessions.remove(historyNum, session);
            throw new InterviewProcessingException("진행 중인 면접이 만료되었습니다. 새 면접을 시작해 주세요.", true, false);
        }
    }

    @Override
    public InterviewSessionResponseDTO getSession(int userNum, int historyNum) {
        ensureAvailable();
        InterviewSession session = sessions.get(historyNum);
        if (session == null || session.userNum != userNum) {
            throw new InterviewProcessingException("이전 면접을 찾을 수 없습니다. 만료되었거나 서버가 재시작되었습니다.", true, false);
        }
        synchronized (session) {
            ensureActive(session, historyNum);
            if (session.failed) throw new InterviewProcessingException("이전 면접 처리에 실패했습니다. 새 면접을 시작해 주세요.", true, false);
            boolean completed = session.questionAnswers.size() == 5;
            var response = new InterviewSessionResponseDTO();
            response.setHistoryNum(historyNum);
            response.setSettings(session.settings);
            // 현재 답변할 문항까지만 공개한다.
            response.setQuestions(List.copyOf(session.questions.subList(0,
                    Math.min(session.questionAnswers.size() + 1, session.questions.size()))));
            response.setAnswers(List.copyOf(session.questionAnswers));
            response.setCompleted(completed);
            response.setAnswerLocked(!completed && session.evaluatedAnswer != null);
            response.setPendingAnswer(session.evaluatedAnswer == null ? null : session.evaluatedAnswer.getAnswer());
            if (completed) response.setResult(createResultResponse(historyNum, session.evaluation));
            response.setAnswerTimeLimitSeconds(ANSWER_TIME_LIMIT_SECONDS);
            response.setQuestionDeadline(session.questionDeadline);
            response.setExpiresAt(session.expiresAt);
            response.setServerTime(clock.millis());
            return response;
        }
    }

    @Override
    public void discardSession(int userNum, int historyNum) {
        ensureAvailable();
        InterviewSession session = sessions.get(historyNum);
        if (session == null) return;
        if (session.userNum != userNum) throw new IllegalArgumentException("해당 면접에 접근할 수 없습니다.");
        synchronized (session) { sessions.remove(historyNum, session); }
        starts.entrySet().removeIf(entry -> entry.getValue().response != null
                && entry.getValue().response.getHistoryNum() == historyNum);
    }

    @Override
    public InterviewDocumentResponseDTO getInterviewDocuments(int userNum) {
        ensureAvailable();
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
        ensureAvailable();
        validateDocumentNumbers(resumeNum, portfolioNum, letterNum);

        LlmInterviewContextDTO context = new LlmInterviewContextDTO();
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
        ensureAvailable();
        validateStartRequest(request);
        if (request.getRequestId() == null) return createSession(userNum, request);
        if (!request.getRequestId().matches("[a-fA-F0-9-]{36}")) {
            throw new IllegalArgumentException("면접 시작 요청 번호가 올바르지 않습니다.");
        }
        String key = userNum + ":" + request.getRequestId();
        StartAttempt attempt = starts.computeIfAbsent(key, ignored -> new StartAttempt(request, expirationAt(clock.millis())));
        synchronized (attempt) {
            ensureAvailable();
            if (!attempt.fingerprint.equals(startFingerprint(request))) {
                throw new IllegalArgumentException("같은 요청 번호로 면접 설정을 변경할 수 없습니다.");
            }
            if (attempt.response != null) {
                InterviewSession session = sessions.get(attempt.response.getHistoryNum());
                if (session == null || session.isExpired(clock.millis())) {
                    throw new InterviewProcessingException("면접이 만료되었습니다. 새 면접을 시작해 주세요.", true, false);
                }
                attempt.response.setServerTime(clock.millis());
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
        long expiresAt = expirationAt(clock.millis());
        LlmInterviewContextDTO context = getLlmInterviewContext(
                userNum, request.getResumeNum(), request.getPortfolioNum(), request.getLetterNum());
        context.setRecruitment(request.getRecruitment());

        LlmQuestionRequestDTO questionRequest = new LlmQuestionRequestDTO();
        questionRequest.setContext(context);
        questionRequest.setInterviewDifficulty(request.getInterviewDifficulty());
        questionRequest.setQuestionAnswers(Collections.emptyList());

        LlmQuestionResponseDTO generated;
        try {
            generated = interviewGenerator.generateDocumentQuestions(questionRequest);
        } catch (RuntimeException exception) {
            context.setPortfolioPdf(null);
            throw exception;
        }
        List<InterviewQuestionDTO> documentQuestions = validateDocumentQuestions(generated);
        ensureAvailable();
        if (clock.millis() >= expiresAt) {
            throw new InterviewProcessingException("면접 준비 중 보관 기한이 만료되었습니다. 새 면접을 시작해 주세요.", true, false);
        }
        int historyNum = interviewDAO.selectNextHistoryNum();
        InterviewSession session = new InterviewSession(userNum, request, context, expiresAt, clock.millis());
        session.questions.addAll(documentQuestions);
        sessions.put(historyNum, session);
        // 번호 발급이 지연되거나 정리 작업과 겹쳐도 만료된 세션을 공개하지 않는다.
        try {
            ensureActive(session, historyNum);
        } catch (RuntimeException exception) {
            sessions.remove(historyNum, session);
            throw exception;
        }

        InterviewStartResponseDTO response = new InterviewStartResponseDTO();
        response.setHistoryNum(historyNum);
        response.setQuestions(List.of(documentQuestions.get(0)));
        response.setAnswerTimeLimitSeconds(ANSWER_TIME_LIMIT_SECONDS);
        response.setQuestionDeadline(session.questionDeadline);
        response.setExpiresAt(session.expiresAt);
        response.setServerTime(clock.millis());
        return response;
    }

    @Override
    public InterviewAnswerResponseDTO submitAnswer(
            int userNum, int historyNum, InterviewAnswerRequestDTO request) {
        ensureAvailable();
        InterviewSession session = sessions.get(historyNum);
        if (session == null || session.userNum != userNum) {
            throw new InterviewProcessingException("진행 중인 면접을 찾을 수 없습니다. 새 면접을 시작해 주세요.", true, false);
        }

        synchronized (session) {
            ensureActive(session, historyNum);

            if (request != null && request.getQuestionOrder() > 0
                    && request.getQuestionOrder() <= session.questionAnswers.size()) {
                int answeredIndex = request.getQuestionOrder() - 1;
                String submittedAnswer = request.getAnswer() == null ? "" : request.getAnswer();
                if (!session.questionAnswers.get(answeredIndex).getAnswer().equals(submittedAnswer)) {
                    throw new IllegalArgumentException("이미 제출한 답변은 변경할 수 없습니다.");
                }
                InterviewAnswerResponseDTO cached = session.responses.get(answeredIndex);
                stampResponse(cached, session);
                return cached;
            }
            if (session.failed) {
                throw new InterviewProcessingException("면접 처리에 실패했습니다. 새 면접을 시작해 주세요.", true, false);
            }

            int expectedOrder = session.questionAnswers.size() + 1;
            if (request != null) {
                request.setTimedOut(session.hasAnswerExpired(clock.millis()));
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
                        session.context.setPortfolioPdf(null);
                        throw generationFailure(exception);
                    }
                    ensureActive(session, historyNum);
                    session.questions.add(nextQuestion);
                }
                session.questionAnswers.add(currentAnswer);
                session.restartAnswerTimer(clock.millis());
                response.setCompleted(false);
                response.setNextQuestion(nextQuestion);
                session.responses.add(response);
                stampResponse(response, session);
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
                } finally {
                    session.context.setPortfolioPdf(null);
                }
            }
            InterviewHistoryDTO history = createHistory(historyNum, userNum, session);
            ensureActive(session, historyNum);
            setHistoryQuestionAnswer(history, session.evaluatedAnswer);
            InterviewResultDTO result = createResult(historyNum, userNum, session.evaluation);
            try {
                saveInterview(history, result);
            } catch (RuntimeException exception) {
                log.warn("Interview history={} save failed type={}", historyNum, exception.getClass().getSimpleName());
                if (isPermanentStorageFailure(exception)) {
                    session.failed = true;
                    throw new InterviewProcessingException(
                            "평가 결과를 저장할 수 없습니다. 관리자에게 문의해 주세요.", true, false);
                }
                throw new InterviewProcessingException("평가는 완료됐지만 결과를 저장하지 못했습니다. 같은 답변으로 다시 제출해 주세요.", false, true);
            }

            session.questionAnswers.add(session.evaluatedAnswer);
            response.setCompleted(true);
            response.setResult(createResultResponse(historyNum, session.evaluation));
            session.responses.add(response);
            stampResponse(response, session);
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

    private void stampResponse(InterviewAnswerResponseDTO response, InterviewSession session) {
        response.setQuestionDeadline(session.questionDeadline);
        response.setExpiresAt(session.expiresAt);
        response.setServerTime(clock.millis());
    }

    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Seoul")
    public void expireDailySessions() { removeInactiveSessions(); }

    @Scheduled(fixedDelay = 60000)
    public void removeInactiveSessions() {
        long now = clock.millis();
        int removedCount = 0;

        for (Map.Entry<Integer, InterviewSession> entry : sessions.entrySet()) {
            InterviewSession session = entry.getValue();
            if (session.isExpired(now) && sessions.remove(entry.getKey(), session)) {
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("보관 기한이 만료된 면접 세션 {}건을 정리했습니다.", removedCount);
        }
        for (var entry : starts.entrySet()) {
            StartAttempt attempt = entry.getValue();
            if (now >= attempt.expiresAt) {
                starts.remove(entry.getKey(), attempt);
            }
        }
    }

    private InterviewProcessingException generationFailure(RuntimeException exception) {
        String message = exception instanceof InterviewProcessingException
                ? exception.getMessage() : "AI 면접 처리에 실패했습니다. 새 면접을 시작해 주세요.";
        return new InterviewProcessingException(message, true, false);
    }

    private static List<Object> startFingerprint(InterviewStartRequestDTO request) {
        var recruitment = request.getRecruitment();
        return Arrays.asList(request.getResumeNum(), request.getPortfolioNum(), request.getLetterNum(),
                request.getInterviewDifficulty(),
                recruitment == null ? null : recruitment.getRecruitmentNum(),
                recruitment == null ? null : recruitment.getCompanyName(),
                recruitment == null ? null : recruitment.getTitle(),
                recruitment == null ? null : recruitment.getJobName(),
                recruitment == null ? null : recruitment.getLocationName(),
                recruitment == null ? null : recruitment.getExperienceLevel());
    }

    private static class StartAttempt {
        private final long expiresAt;
        private final List<Object> fingerprint;
        private volatile InterviewStartResponseDTO response;
        private RuntimeException failure;
        private StartAttempt(InterviewStartRequestDTO request, long expiresAt) {
            fingerprint = startFingerprint(request);
            this.expiresAt = expiresAt;
        }
    }

    private InterviewQuestionDTO generateFollowUpQuestion(
            InterviewSession session, int questionOrder, InterviewQuestionAnswerDTO currentAnswer) {
        LlmFollowUpRequestDTO request = new LlmFollowUpRequestDTO();
        request.setContext(session.context);
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(answersIncluding(session, currentAnswer));
        return interviewGenerator.generateFollowUpQuestion(questionOrder, request);
    }

    private LlmEvaluationResponseDTO evaluate(
            InterviewSession session, int userNum, InterviewQuestionAnswerDTO currentAnswer) {
        LlmEvaluationRequestDTO request = new LlmEvaluationRequestDTO();
        request.setContext(session.context);
        request.setInterviewDifficulty(session.settings.getInterviewDifficulty());
        request.setQuestionAnswers(answersIncluding(session, currentAnswer));
        request.setPreviousResult(interviewDAO.selectLatestInterviewResultByUserNum(userNum));
        LlmEvaluationResponseDTO evaluation = interviewGenerator.evaluate(request);
        validateEvaluation(evaluation);
        evaluation.setTotalScore(Math.round((
                evaluation.getDocumentConsistencyScore()
                + evaluation.getExpertiseScore()
                + evaluation.getProblemSolvingScore()
                + evaluation.getLogicScore()
                + evaluation.getCommunicationScore()) / 5.0 * 10.0) / 10.0);
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
        result.setDocumentConsistencyScore(evaluation.getDocumentConsistencyScore());
        result.setProblemSolvingScore(evaluation.getProblemSolvingScore());
        result.setExpertiseScore(evaluation.getExpertiseScore());
        result.setLogicScore(evaluation.getLogicScore());
        result.setCommunicationScore(evaluation.getCommunicationScore());
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
        response.setDocumentConsistencyScore(evaluation.getDocumentConsistencyScore());
        response.setProblemSolvingScore(evaluation.getProblemSolvingScore());
        response.setExpertiseScore(evaluation.getExpertiseScore());
        response.setLogicScore(evaluation.getLogicScore());
        response.setCommunicationScore(evaluation.getCommunicationScore());
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
        if (request.getInterviewDifficulty() == null || request.getInterviewDifficulty().isBlank()) {
            throw new IllegalArgumentException("면접 난이도를 선택해 주세요.");
        }
        if (!List.of("EASY", "NORMAL", "HARD").contains(request.getInterviewDifficulty())) {
            throw new IllegalArgumentException("면접 난이도가 올바르지 않습니다.");
        }
        var recruitment = request.getRecruitment();
        if (recruitment != null) {
            if (recruitment.getRecruitmentNum() != null && recruitment.getRecruitmentNum() <= 0) {
                throw new IllegalArgumentException("채용 공고 번호가 올바르지 않습니다.");
            }
            validateRecruitmentField(recruitment.getCompanyName(), 200);
            validateRecruitmentField(recruitment.getTitle(), 400);
            validateRecruitmentField(recruitment.getJobName(), 300);
            validateRecruitmentField(recruitment.getLocationName(), 200);
            validateRecruitmentField(recruitment.getExperienceLevel(), 100);
        }
    }

    private void validateRecruitmentField(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException("채용 공고 정보가 허용 길이를 초과했습니다.");
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
                || question.getQuestionText().length() > InterviewTextLimits.QUESTION) {
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
        if (answer != null && answer.length() > InterviewTextLimits.ANSWER) {
            throw new IllegalArgumentException("답변은 1000자 이하로 입력해 주세요.");
        }
    }

    private void validateEvaluation(LlmEvaluationResponseDTO evaluation) {
        if (evaluation == null) {
            throw new IllegalStateException("면접 평가 결과가 없습니다.");
        }
        validateScore(evaluation.getDocumentConsistencyScore());
        validateScore(evaluation.getProblemSolvingScore());
        validateScore(evaluation.getExpertiseScore());
        validateScore(evaluation.getLogicScore());
        validateScore(evaluation.getCommunicationScore());
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
        if (feedback == null || feedback.isBlank() || feedback.length() > InterviewTextLimits.FEEDBACK) {
            throw new IllegalStateException("면접 평가 문구는 1자 이상 250자 이하여야 합니다.");
        }
    }

    private boolean isPermanentStorageFailure(RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException
                    || cause instanceof SQLException sqlException && sqlException.getErrorCode() == 12899) {
                return true;
            }
        }
        return false;
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
        private final long expiresAt;
        private long questionDeadline;

        private InterviewSession(
                int userNum, InterviewStartRequestDTO settings, LlmInterviewContextDTO context,
                long expiresAt, long now) {
            this.userNum = userNum;
            this.settings = settings;
            this.context = context;
            this.expiresAt = expiresAt;
            restartAnswerTimer(now);
        }

        private boolean isExpired(long now) {
            return now >= expiresAt;
        }

        private boolean hasAnswerExpired(long now) {
            return now >= questionDeadline;
        }

        private void restartAnswerTimer(long now) {
            questionDeadline = now + TimeUnit.SECONDS.toMillis(ANSWER_TIME_LIMIT_SECONDS);
        }
    }

}
