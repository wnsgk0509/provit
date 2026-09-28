package com.provit.service.interview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.interview.InterviewAnswerRequestDTO;
import com.provit.dto.interview.InterviewAnswerResponseDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewQuestionDTO;
import com.provit.dto.interview.InterviewRecruitmentDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.LlmEvaluationRequestDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;
import com.provit.dto.interview.LlmFollowUpRequestDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;
import com.provit.dto.interview.LlmQuestionResponseDTO;
import com.provit.service.interview.generator.InterviewGenerator;
import com.provit.service.interview.impl.InterviewServiceImpl;

public class InterviewFourCallFlowTest {

    @Test
    public void fiveAnswersUseOneBatchTwoFollowUpsAndOneEvaluation() {
        RecordingGenerator generator = new RecordingGenerator();
        AtomicInteger savedHistories = new AtomicInteger();
        InterviewDAO dao = fakeDao(savedHistories);
        InterviewServiceImpl service = service(dao, generator, new InterviewPersistenceService(dao));
        var started = service.startInterview(7, settings());
        int historyNum = started.getHistoryNum();
        assertEquals(1, started.getQuestions().size());
        assertEquals(1, generator.batchCalls);

        for (int order = 1; order <= 5; order++) {
            InterviewAnswerRequestDTO answer = answer(order);
            InterviewAnswerResponseDTO response = service.submitAnswer(7, historyNum, answer);
            if (order < 5) {
                assertFalse(response.isCompleted());
                assertEquals(order + 1, response.getNextQuestion().getQuestionOrder());
            } else {
                assertTrue(response.isCompleted());
                assertEquals(80.0, response.getResult().getTotalScore(), 0.001);
            }
            assertSame(response, service.submitAnswer(7, historyNum, answer));
            if (order <= 2) {
                assertEquals(1, generator.batchCalls);
                assertEquals(0, generator.followUpCalls);
            }
        }
        assertEquals(1, generator.batchCalls);
        assertEquals(2, generator.followUpCalls);
        assertEquals(1, generator.evaluationCalls);
        assertEquals(1, savedHistories.get());
        var restoredResult = service.getSession(7, historyNum);
        assertTrue(restoredResult.isCompleted());
        assertEquals(5, restoredResult.getAnswers().size());
        assertEquals(80.0, restoredResult.getResult().getTotalScore(), 0.001);
        assertEquals(60.0, restoredResult.getResult().getDocumentConsistencyScore(), 0.001);
        assertEquals(70.0, restoredResult.getResult().getExpertiseScore(), 0.001);
        assertEquals(80.0, restoredResult.getResult().getProblemSolvingScore(), 0.001);
        assertEquals(90.0, restoredResult.getResult().getLogicScore(), 0.001);
        assertEquals(100.0, restoredResult.getResult().getCommunicationScore(), 0.001);
        assertEquals(1, generator.evaluationCalls);
    }

    @Test
    public void failedFollowUpCannotCauseAnExtraGeneratorCall() {
        RecordingGenerator generator = new RecordingGenerator() {
            @Override
            public InterviewQuestionDTO generateFollowUpQuestion(int order, LlmFollowUpRequestDTO request) {
                followUpCalls++;
                throw new IllegalStateException("질문 생성 실패");
            }
        };
        InterviewDAO dao = fakeDao(new AtomicInteger());
        InterviewServiceImpl service = service(dao, generator, new InterviewPersistenceService(dao));
        int historyNum = service.startInterview(7, settings()).getHistoryNum();
        service.submitAnswer(7, historyNum, answer(1));
        service.submitAnswer(7, historyNum, answer(2));

        assertThrows(IllegalStateException.class, () -> service.submitAnswer(7, historyNum, answer(3)));
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(7, historyNum, answer(3)));
        assertEquals(1, generator.followUpCalls);
    }

    @Test
    public void retryAfterSaveFailureReusesEvaluation() {
        RecordingGenerator generator = new RecordingGenerator();
        InterviewDAO dao = fakeDao(new AtomicInteger());
        InterviewPersistenceService persistence = new InterviewPersistenceService(dao) {
            private boolean failOnce = true;

            @Override
            public int save(InterviewHistoryDTO history, InterviewResultDTO result) {
                if (failOnce) {
                    failOnce = false;
                    throw new IllegalStateException("저장 실패");
                }
                return super.save(history, result);
            }
        };
        InterviewServiceImpl service = service(dao, generator, persistence);
        int historyNum = service.startInterview(7, settings()).getHistoryNum();
        for (int order = 1; order <= 4; order++) {
            service.submitAnswer(7, historyNum, answer(order));
        }

        assertThrows(IllegalStateException.class, () -> service.submitAnswer(7, historyNum, answer(5)));
        var resumed = service.getSession(7, historyNum);
        assertTrue(resumed.isAnswerLocked());
        assertEquals("답변 5", resumed.getPendingAnswer());
        assertEquals(4, resumed.getAnswers().size());
        InterviewAnswerRequestDTO changed = answer(5);
        changed.setAnswer("변경한 답변");
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(7, historyNum, changed));
        assertTrue(service.submitAnswer(7, historyNum, answer(5)).isCompleted());
        assertEquals(1, generator.evaluationCalls);
    }

    private InterviewServiceImpl service(
            InterviewDAO dao, InterviewGenerator generator, InterviewPersistenceService persistence) {
        return service(dao, generator, persistence,
                Clock.fixed(Instant.parse("2026-09-28T03:00:00Z"), ZoneOffset.UTC));
    }

    private InterviewServiceImpl service(InterviewDAO dao, InterviewGenerator generator,
            InterviewPersistenceService persistence, Clock clock) {
        return new InterviewServiceImpl(dao, generator, persistence, new InterviewDocumentInputBuilder(null), clock);
    }

    @Test
    public void resumeAfterLongAbsencePreservesProgressWithoutGeneratingAgain() {
        var clock = new MutableClock("2026-09-28T03:00:00Z");
        var generator = new RecordingGenerator();
        var dao = fakeDao(new AtomicInteger());
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        var request = settings();
        var recruitment = new InterviewRecruitmentDTO();
        recruitment.setCompanyName("지원 회사");
        request.setRecruitment(recruitment);
        int id = service.startInterview(7, request).getHistoryNum();
        service.submitAnswer(7, id, answer(1));
        var before = service.getSession(7, id);
        clock.set("2026-09-28T05:00:00Z");
        service.removeInactiveSessions();
        var resumed = service.getSession(7, id);
        assertEquals(1, resumed.getAnswers().size());
        assertEquals("답변 1", resumed.getAnswers().get(0).getAnswer());
        assertEquals(2, resumed.getQuestions().size());
        assertEquals(before.getQuestionDeadline(), resumed.getQuestionDeadline());
        assertEquals("지원 회사", resumed.getSettings().getRecruitment().getCompanyName());
        assertEquals(Instant.parse("2026-09-28T14:55:00Z").toEpochMilli(), resumed.getExpiresAt());
        assertEquals(1, generator.batchCalls);
        assertEquals(0, generator.followUpCalls);
        var timedOut = answer(2);
        timedOut.setTimedOut(false);
        service.submitAnswer(7, id, timedOut);
        assertTrue(service.getSession(7, id).getAnswers().get(1).isTimedOut());
    }

    @Test
    public void otherUserCannotResumeOrDiscardAndFutureQuestionsAreHidden() {
        var generator = new RecordingGenerator();
        var dao = fakeDao(new AtomicInteger());
        var service = service(dao, generator, new InterviewPersistenceService(dao));
        int id = service.startInterview(7, settings()).getHistoryNum();
        assertEquals(1, service.getSession(7, id).getQuestions().size());
        assertThrows(InterviewProcessingException.class, () -> service.getSession(8, id));
        assertThrows(IllegalArgumentException.class, () -> service.discardSession(8, id));
        assertEquals(1, service.getSession(7, id).getQuestions().size());
        service.discardSession(7, id);
        service.discardSession(7, id);
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, id));
    }

    @Test
    public void maintenanceUsesKoreanTimeWithInclusiveStartAndExclusiveMidnight() {
        var clock = new MutableClock("2026-09-28T14:54:59.999Z");
        var dao = fakeDao(new AtomicInteger());
        var service = service(dao, new RecordingGenerator(), new InterviewPersistenceService(dao), clock);
        assertFalse(service.getAvailability().isMaintenance());
        assertEquals(Instant.parse("2026-09-28T14:55:00Z").toEpochMilli(), service.getAvailability().getNextChangeAt());
        clock.set("2026-09-28T14:55:00Z");
        assertTrue(service.getAvailability().isMaintenance());
        assertEquals(Instant.parse("2026-09-28T15:00:00Z").toEpochMilli(), service.getAvailability().getNextChangeAt());
        clock.set("2026-09-28T14:59:59.999Z");
        assertTrue(service.getAvailability().isMaintenance());
        clock.set("2026-09-28T15:00:00Z");
        assertFalse(service.getAvailability().isMaintenance());
    }

    @Test
    public void maintenanceBlocksSessionAndGenerationApisAndDailyCleanupClearsMemory() {
        var clock = new MutableClock("2026-09-28T14:54:00Z");
        var generator = new RecordingGenerator();
        var saves = new AtomicInteger();
        var dao = fakeDao(saves);
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        var request = settings();
        request.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        int id = service.startInterview(7, request).getHistoryNum();
        clock.set("2026-09-28T14:55:00Z");
        assertThrows(InterviewMaintenanceException.class, () -> service.startInterview(7, request));
        assertThrows(InterviewMaintenanceException.class, () -> service.getInterviewDocuments(7));
        assertThrows(InterviewMaintenanceException.class, () -> service.getLlmInterviewContext(7, 1, 0, 2));
        assertThrows(InterviewMaintenanceException.class, () -> service.getSession(7, id));
        assertThrows(InterviewMaintenanceException.class, () -> service.submitAnswer(7, id, answer(1)));
        assertThrows(InterviewMaintenanceException.class, () -> service.discardSession(7, id));
        service.expireDailySessions();
        clock.set("2026-09-28T15:00:00Z");
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, id));
        assertEquals(0, saves.get());
        service.startInterview(7, request);
        assertEquals(2, generator.batchCalls);
    }

    @Test
    public void missedCleanupDoesNotAllowExpiredProgressOnNextDay() {
        var clock = new MutableClock("2026-09-28T14:54:00Z");
        var dao = fakeDao(new AtomicInteger());
        var generator = new RecordingGenerator();
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        int id = service.startInterview(7, settings()).getHistoryNum();
        clock.set("2026-09-28T15:01:00Z");
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, id));
        assertThrows(InterviewProcessingException.class, () -> service.submitAnswer(7, id, answer(1)));
        assertEquals(1, generator.batchCalls);
    }

    @Test
    public void generationFinishingAfterMaintenanceCannotPublishAnExpiredSession() {
        var clock = new MutableClock("2026-09-28T14:54:59Z");
        RecordingGenerator generator = new RecordingGenerator() {
            @Override
            public LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request) {
                var result = super.generateDocumentQuestions(request);
                clock.set("2026-09-28T15:00:01Z");
                return result;
            }
        };
        var dao = fakeDao(new AtomicInteger());
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        assertThrows(InterviewProcessingException.class, () -> service.startInterview(7, settings()));
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, 17));
    }

    @Test
    public void restartedServerHasNoResumableMemory() {
        var generator = new RecordingGenerator();
        var dao = fakeDao(new AtomicInteger());
        int id = service(dao, generator, new InterviewPersistenceService(dao))
                .startInterview(7, settings()).getHistoryNum();
        var restarted = service(dao, generator, new InterviewPersistenceService(dao));
        assertThrows(InterviewProcessingException.class, () -> restarted.getSession(7, id));
    }

    @Test
    public void delayedNumberAllocationCannotResurrectProgressAfterDailyCleanup() {
        var clock = new MutableClock("2026-09-28T14:54:59Z");
        var delegate = fakeDao(new AtomicInteger());
        var dao = (InterviewDAO) Proxy.newProxyInstance(InterviewDAO.class.getClassLoader(),
                new Class<?>[] { InterviewDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("selectNextHistoryNum")) clock.set("2026-09-28T15:00:01Z");
                    return method.invoke(delegate, args);
                });
        var service = service(dao, new RecordingGenerator(), new InterviewPersistenceService(dao), clock);
        assertThrows(InterviewProcessingException.class, () -> service.startInterview(7, settings()));
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, 17));
    }

    @Test
    public void followUpCompletingDuringMaintenanceIsDiscarded() {
        var clock = new MutableClock("2026-09-28T14:54:59Z");
        var generator = new RecordingGenerator() {
            @Override
            public InterviewQuestionDTO generateFollowUpQuestion(int order, LlmFollowUpRequestDTO request) {
                var result = super.generateFollowUpQuestion(order, request);
                clock.set("2026-09-28T14:55:00Z");
                return result;
            }
        };
        var dao = fakeDao(new AtomicInteger());
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        int id = service.startInterview(7, settings()).getHistoryNum();
        service.submitAnswer(7, id, answer(1));
        service.submitAnswer(7, id, answer(2));
        assertThrows(InterviewMaintenanceException.class, () -> service.submitAnswer(7, id, answer(3)));
        service.expireDailySessions();
        clock.set("2026-09-28T15:00:00Z");
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, id));
        assertEquals(1, generator.followUpCalls);
    }

    @Test
    public void evaluationCompletingAfterExpiryCannotSaveAResult() {
        var clock = new MutableClock("2026-09-28T14:54:59Z");
        var generator = new RecordingGenerator() {
            @Override
            public LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request) {
                var result = super.evaluate(request);
                clock.set("2026-09-28T15:00:01Z");
                return result;
            }
        };
        var saves = new AtomicInteger();
        var dao = fakeDao(saves);
        var service = service(dao, generator, new InterviewPersistenceService(dao), clock);
        int id = service.startInterview(7, settings()).getHistoryNum();
        for (int order = 1; order <= 4; order++) service.submitAnswer(7, id, answer(order));
        assertThrows(InterviewProcessingException.class, () -> service.submitAnswer(7, id, answer(5)));
        assertEquals(0, saves.get());
        assertThrows(InterviewProcessingException.class, () -> service.getSession(7, id));
    }

    private static class MutableClock extends Clock {
        private Instant now;
        MutableClock(String instant) { set(instant); }
        void set(String instant) { now = Instant.parse(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }

    @Test
    public void changedRecruitmentCannotReuseStartRequestId() {
        RecordingGenerator generator = new RecordingGenerator();
        InterviewDAO dao = fakeDao(new AtomicInteger());
        InterviewServiceImpl service = service(dao, generator, new InterviewPersistenceService(dao));
        var original = settings();
        original.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        var recruitment = new InterviewRecruitmentDTO();
        recruitment.setRecruitmentNum(42L);
        recruitment.setCompanyName("테스트 기업");
        recruitment.setJobName("백엔드 개발");
        original.setRecruitment(recruitment);
        service.startInterview(7, original);

        var changed = settings();
        changed.setRequestId(original.getRequestId());
        var changedRecruitment = new InterviewRecruitmentDTO();
        changedRecruitment.setRecruitmentNum(42L);
        changedRecruitment.setCompanyName("테스트 기업");
        changedRecruitment.setJobName("프론트엔드 개발");
        changed.setRecruitment(changedRecruitment);
        assertThrows(IllegalArgumentException.class, () -> service.startInterview(7, changed));
        changed.setRecruitment(null);
        assertThrows(IllegalArgumentException.class, () -> service.startInterview(7, changed));
        assertEquals(1, generator.batchCalls);
    }

    @Test
    public void oversizedRecruitmentFailsBeforeQuestionGeneration() {
        RecordingGenerator generator = new RecordingGenerator();
        InterviewDAO dao = fakeDao(new AtomicInteger());
        InterviewServiceImpl service = service(dao, generator, new InterviewPersistenceService(dao));
        var request = settings();
        var recruitment = new InterviewRecruitmentDTO();
        recruitment.setTitle("가".repeat(401));
        request.setRecruitment(recruitment);
        assertThrows(IllegalArgumentException.class, () -> service.startInterview(7, request));
        assertEquals(0, generator.batchCalls);
    }

    private InterviewStartRequestDTO settings() {
        InterviewStartRequestDTO settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setInterviewStyle("ONE_TO_ONE");
        settings.setInterviewDifficulty("NORMAL");
        return settings;
    }

    private InterviewAnswerRequestDTO answer(int order) {
        InterviewAnswerRequestDTO answer = new InterviewAnswerRequestDTO();
        answer.setQuestionOrder(order);
        answer.setAnswer("답변 " + order);
        return answer;
    }

    private InterviewDAO fakeDao(AtomicInteger savedHistories) {
        ResumeDTO resume = new ResumeDTO();
        resume.setResumeTitle("백엔드 이력서");
        CoverLetterDTO coverLetter = new CoverLetterDTO();
        coverLetter.setProblemSolvingExperience("서버 병목 개선");
        return (InterviewDAO) Proxy.newProxyInstance(
                InterviewDAO.class.getClassLoader(), new Class<?>[] { InterviewDAO.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectResumeByResumeNumAndUserNum" -> resume;
                    case "selectCoverLetterByLetterNumAndUserNum" -> coverLetter;
                    case "selectEducationListByResumeNum", "selectCareerListByResumeNum",
                            "selectCertificationListByResumeNum" -> List.of();
                    case "selectNextHistoryNum" -> 17;
                    case "insertInterviewHistory" -> savedHistories.incrementAndGet();
                    case "insertInterviewResult" -> {
                        var result = (InterviewResultDTO) args[0];
                        assertEquals(60.0, result.getDocumentConsistencyScore(), 0.001);
                        assertEquals(70.0, result.getExpertiseScore(), 0.001);
                        assertEquals(80.0, result.getProblemSolvingScore(), 0.001);
                        assertEquals(90.0, result.getLogicScore(), 0.001);
                        assertEquals(100.0, result.getCommunicationScore(), 0.001);
                        assertEquals(80.0, result.getTotalScore(), 0.001);
                        yield 1;
                    }
                    default -> null;
                });
    }

    private static class RecordingGenerator implements InterviewGenerator {
        int batchCalls;
        int followUpCalls;
        int evaluationCalls;

        @Override
        public LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request) {
            batchCalls++;
            assertTrue(request.getContext().getDocumentText().contains("[이력서]"));
            LlmQuestionResponseDTO response = new LlmQuestionResponseDTO();
            response.setQuestions(List.of(
                    question(1, "DOCUMENT"), question(2, "DOCUMENT"), question(3, "DOCUMENT")));
            return response;
        }

        @Override
        public InterviewQuestionDTO generateFollowUpQuestion(int order, LlmFollowUpRequestDTO request) {
            followUpCalls++;
            assertEquals(order - 1, request.getQuestionAnswers().size());
            return question(order, "FOLLOW_UP");
        }

        @Override
        public LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request) {
            evaluationCalls++;
            assertEquals(5, request.getQuestionAnswers().size());
            LlmEvaluationResponseDTO result = new LlmEvaluationResponseDTO();
            result.setDocumentConsistencyScore(60);
            result.setProblemSolvingScore(80);
            result.setExpertiseScore(70);
            result.setLogicScore(90);
            result.setCommunicationScore(100);
            result.setStrengths("근거를 제시했습니다.");
            result.setWeaknesses("결과가 모호했습니다.");
            result.setComparison("이전 면접 기록 없음");
            result.setImprovements("결과를 수치로 설명하세요.");
            return result;
        }

        private InterviewQuestionDTO question(int order, String type) {
            InterviewQuestionDTO question = new InterviewQuestionDTO();
            question.setQuestionOrder(order);
            question.setQuestionType(type);
            question.setQuestionText("질문 " + order);
            return question;
        }
    }
}
