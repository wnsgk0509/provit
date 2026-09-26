package com.provit.service.interview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.interview.InterviewAnswerRequestDTO;
import com.provit.dto.interview.InterviewAnswerResponseDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewQuestionDTO;
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
        InterviewAnswerRequestDTO changed = answer(5);
        changed.setAnswer("변경한 답변");
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(7, historyNum, changed));
        assertTrue(service.submitAnswer(7, historyNum, answer(5)).isCompleted());
        assertEquals(1, generator.evaluationCalls);
    }

    private InterviewServiceImpl service(
            InterviewDAO dao, InterviewGenerator generator, InterviewPersistenceService persistence) {
        return new InterviewServiceImpl(dao, generator, persistence, new InterviewDocumentInputBuilder(null));
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
                    case "insertInterviewResult" -> 1;
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
            result.setConfidenceScore(80);
            result.setPersistenceScore(80);
            result.setExpertiseScore(80);
            result.setLogicScore(80);
            result.setDeliveryScore(80);
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
