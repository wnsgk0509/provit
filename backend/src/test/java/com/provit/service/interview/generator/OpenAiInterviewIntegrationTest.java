package com.provit.service.interview.generator;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.interview.*;
import com.provit.service.interview.InterviewDocumentInputBuilder;
import com.provit.service.interview.InterviewPersistenceService;
import com.provit.service.interview.InterviewProcessingException;
import com.provit.service.interview.impl.InterviewServiceImpl;
import com.provit.service.document.storage.PortfolioFileStorage;

public class OpenAiInterviewIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private URI endpoint;
    private final List<JsonNode> requests = new ArrayList<>();
    private int status = 200;
    private String overrideResponse;

    @Before
    public void startFixture() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
        server.createContext("/v1/responses", exchange -> {
            JsonNode request = mapper.readTree(exchange.getRequestBody().readAllBytes());
            requests.add(request);
            String stage = request.path("text").path("format").path("name").asText();
            String generated = switch (stage) {
                case "interview_document" -> "{\"questions\":[\"자기소개서의 고객 분석 경험에서 본인의 역할은 무엇인가요?\",\""
                        + (request.path("input").isArray() ? "포트폴리오의 고객 분석 기준을 선택한 이유는 무엇인가요?"
                                : "자기소개서의 지원 동기를 본인의 강점과 연결해 설명해 주세요.")
                        + "\",\"콘텐츠마케팅에서 고객 세분화의 원리는 무엇인가요?\"]}";
                case "interview_follow_up_4" ->
                        "{\"questionText\":\"콘텐츠 반응률이 목표에 미치지 못한다면 원인을 어떻게 파악하시겠습니까?\"}";
                case "interview_follow_up_5" ->
                        "{\"questionText\":\"1번 답변에서 고객 분석 기준을 선택한 구체적인 근거는 무엇인가요?\"}";
                default -> "{\"documentConsistencyScore\":70,\"problemSolvingScore\":75,\"expertiseScore\":80,"
                        + "\"logicScore\":85,\"communicationScore\":90,\"strengths\":\"1번에서 기여를 설명했습니다.\","
                        + "\"weaknesses\":\"3번의 성과 근거가 부족합니다.\",\"improvements\":\"성과 비교 조건을 설명하세요.\","
                        + "\"comparison\":\"이전 평가보다 논리력 점수가 높습니다.\"}";
            };
            ObjectNode response = mapper.createObjectNode().put("status", "completed");
            response.putArray("output").addObject().put("type", "message")
                    .putArray("content").addObject().put("type", "output_text").put("text", generated);
            response.putObject("usage").put("input_tokens", 100).put("output_tokens", 200);
            byte[] body = (overrideResponse == null ? mapper.writeValueAsString(response) : overrideResponse)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @After
    public void stopFixture() { server.stop(0); }

    @Test
    public void fullInterviewUsesFourRequestsWithOnlyNecessaryInputs() throws Exception {
        assertFullInterview(null, "NORMAL");
    }

    @Test
    public void easyInterviewKeepsBasicQuestionGuidanceAcrossAllQuestionStages() throws Exception {
        assertFullInterview(null, "EASY");
    }

    @Test
    public void hardInterviewKeepsCounterexampleAndConstraintGuidanceAcrossAllQuestionStages() throws Exception {
        assertFullInterview(null, "HARD");
    }

    @Test
    public void selectedRecruitmentReachesInitialQuestionsBothFollowUpsAndEvaluation() throws Exception {
        var recruitment = mapper.readValue("""
                {"recruitmentNum":42,"companyName":"테스트 기업","title":"퍼포먼스마케터 채용",
                 "jobName":"퍼포먼스마케팅,SNS마케팅","locationName":"서울","experienceLevel":"신입"}
                """, InterviewRecruitmentDTO.class);
        assertFullInterview(recruitment, "NORMAL");
    }

    private void assertFullInterview(InterviewRecruitmentDTO recruitment, String difficulty) throws Exception {
        AtomicInteger saves = new AtomicInteger();
        InterviewDAO dao = dao(saves);
        var service = new InterviewServiceImpl(dao, generator("sk-local-test-only"),
                new InterviewPersistenceService(dao), new InterviewDocumentInputBuilder(null), testClock());
        InterviewStartRequestDTO settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setInterviewDifficulty(difficulty);
        settings.setRecruitment(recruitment);
        settings.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        var started = service.startInterview(7, settings);
        assertEquals(1, started.getQuestions().get(0).getQuestionOrder());
        assertEquals("DOCUMENT", started.getQuestions().get(0).getQuestionType());
        assertSame(started, service.startInterview(7,
                mapper.readValue(mapper.writeValueAsString(settings), InterviewStartRequestDTO.class)));
        assertEquals(1, requests.size());
        for (int order = 1; order <= 5; order++) {
            var answer = new InterviewAnswerRequestDTO();
            answer.setQuestionOrder(order);
            answer.setAnswer("답변 " + order);
            var response = service.submitAnswer(7, started.getHistoryNum(), answer);
            assertSame(response, service.submitAnswer(7, started.getHistoryNum(), answer));
            if (order == 5) {
                assertEquals(70.0, response.getResult().getDocumentConsistencyScore(), 0.001);
                assertEquals(80.0, response.getResult().getExpertiseScore(), 0.001);
                assertEquals(75.0, response.getResult().getProblemSolvingScore(), 0.001);
                assertEquals(85.0, response.getResult().getLogicScore(), 0.001);
                assertEquals(90.0, response.getResult().getCommunicationScore(), 0.001);
                assertEquals(80.0, response.getResult().getTotalScore(), 0.001);
            }
            else {
                assertEquals(order + 1, response.getNextQuestion().getQuestionOrder());
                assertEquals(order < 3 ? "DOCUMENT" : "FOLLOW_UP", response.getNextQuestion().getQuestionType());
            }
        }
        assertEquals(4, requests.size());
        assertEquals(1, saves.get());
        int[] limits = {900, 600, 600, 1400};
        for (int index = 0; index < 4; index++) {
            JsonNode request = requests.get(index);
            assertEquals("gpt-6-sol", request.path("model").asText());
            assertEquals("medium", request.path("reasoning").path("effort").asText());
            assertFalse(request.has("temperature"));
            assertFalse(request.path("store").asBoolean());
            assertEquals(limits[index], request.path("max_output_tokens").asInt());
            assertTrue(request.path("text").path("format").path("strict").asBoolean());
            JsonNode input = mapper.readTree(request.path("input").asText());
            assertEquals(difficulty, input.path("interviewDifficulty").asText());
            assertFalse(input.has("interviewStyle"));
            assertTrue(request.path("instructions").asText().contains("일대일 면접"));
            assertEquals("14", input.path("occupationCode").asText());
            assertEquals("마케팅·홍보·조사", input.path("occupation").asText());
            assertEquals("310", input.path("jobCode").asText());
            assertEquals("콘텐츠마케팅", input.path("job").asText());
            if (recruitment == null) {
                assertFalse(input.has("recruitment"));
            } else {
                JsonNode target = input.path("recruitment");
                assertEquals(recruitment.getCompanyName(), target.path("companyName").asText());
                assertEquals(recruitment.getTitle(), target.path("title").asText());
                assertEquals(recruitment.getJobName(), target.path("jobName").asText());
                assertEquals(recruitment.getLocationName(), target.path("locationName").asText());
                assertEquals(recruitment.getExperienceLevel(), target.path("experienceLevel").asText());
                assertFalse(target.has("recruitmentNum"));
                assertTrue(request.path("instructions").asText().contains("이번 면접의 우선 기준"));
                if (index == 3) {
                    assertTrue(request.path("instructions").asText().contains("공고의 모집 직무와 경력 조건에 비추어"));
                }
            }
            assertTrue(request.path("instructions").asText().contains("선택한 이력서의 1차 직군"));
            assertFalse(input.has("userNum"));
            if (index == 0) {
                assertTrue(input.path("documentText").asText().contains("[자기소개서]"));
            } else {
                if (index == 3) {
                    JsonNode original = mapper.readTree(requests.get(0).path("input").asText());
                    assertEquals(original.path("documentText"), input.path("documentText"));
                    assertFalse(input.path("hasPortfolio").asBoolean());
                } else {
                    assertFalse(input.has("documentText"));
                }
                assertFalse(input.has("context"));
                assertEquals(index + 2, input.path("questionAnswers").size());
                for (int answerIndex = 0; answerIndex < input.path("questionAnswers").size(); answerIndex++) {
                    JsonNode item = input.path("questionAnswers").get(answerIndex);
                    assertEquals("답변 " + (answerIndex + 1), item.path("answer").asText());
                    assertFalse(item.path("question").asText().isBlank());
                    assertFalse(item.has("questionOrder"));
                    assertFalse(item.has("questionType"));
                    assertFalse(item.has("timedOut"));
                }
            }
            if (index < 3) {
                assertFalse(input.has("styleGuide"));
                JsonNode initial = mapper.readTree(requests.get(0).path("input").asText());
                assertEquals(initial.path("difficultyGuide"), input.path("difficultyGuide"));
                String guide = input.path("difficultyGuide").asText();
                assertTrue(guide.contains("(" + difficulty + ")"));
                assertTrue(guide.contains("직무 지식 질문"));
                assertTrue(guide.contains("문제해결 질문"));
                assertTrue(guide.contains("꼬리질문"));
                if (difficulty.equals("EASY")) assertTrue(guide.contains("입문 수준"));
                if (difficulty.equals("NORMAL")) assertTrue(guide.contains("선택의 이유와 타당성"));
                if (difficulty.equals("HARD")) assertTrue(guide.contains("두 조건이 충돌"));
                assertTrue(request.path("instructions").asText().contains("핵심 검증 지점 하나"));
            }
            if (index == 3) {
                assertFalse(input.has("styleGuide"));
                assertFalse(request.path("instructions").asText().contains("핵심 검증 지점 하나"));
                assertTrue(request.path("instructions").asText().contains("짧다는 이유만으로 감점하지"));
                assertTrue(request.path("instructions").asText().contains("이번 답변을 독립적으로 채점"));
                assertEquals(60.0, input.path("previousResult").path("totalScore").asDouble(), 0.001);
                assertEquals("성과 근거 부족", input.path("previousResult").path("weaknesses").asText());
                assertFalse(input.path("previousResult").has("userNum"));
            }
        }
        assertTrue(requests.get(1).path("instructions").asText().contains("전체를 검토"));
        assertTrue(requests.get(1).path("instructions").asText().contains("직무 관련 문제해결능력 질문을 정확히 1개"));
        assertTrue(requests.get(2).path("instructions").asText().contains("비어 있지 않은 1~4번 답변 중"));
    }

    @Test
    public void portfolioInterviewUsesSameDocumentSnapshotForQuestionsAndEvaluationThenReleasesBytes() throws Exception {
        byte[] pdf = portfolioPdf();
        LlmInterviewContextDTO[] prepared = new LlmInterviewContextDTO[1];
        InterviewDocumentInputBuilder builder = new InterviewDocumentInputBuilder(storage(pdf)) {
            @Override
            public void prepare(LlmInterviewContextDTO context) {
                super.prepare(context);
                prepared[0] = context;
            }
        };
        AtomicInteger saves = new AtomicInteger();
        InterviewDAO dao = dao(saves, portfolio());
        var service = new InterviewServiceImpl(dao, generator("sk-local-test-only"),
                new InterviewPersistenceService(dao), builder, testClock());
        var settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setPortfolioNum(3);
        settings.setInterviewDifficulty("NORMAL");
        settings.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        var started = service.startInterview(7, settings);
        assertTrue(started.getQuestions().get(0).getQuestionText().contains("자기소개서"));
        assertFalse(started.getQuestions().get(0).getQuestionText().contains("포트폴리오"));
        assertArrayEquals(pdf, prepared[0].getPortfolioPdf());
        assertSame(started, service.startInterview(7, settings));
        for (int order = 1; order <= 5; order++) {
            var answer = new InterviewAnswerRequestDTO();
            answer.setQuestionOrder(order);
            answer.setAnswer("답변 " + order);
            var response = service.submitAnswer(7, started.getHistoryNum(), answer);
            if (order == 1) assertTrue(response.getNextQuestion().getQuestionText().contains("포트폴리오"));
            if (order < 5) assertArrayEquals(pdf, prepared[0].getPortfolioPdf());
            else assertNull(prepared[0].getPortfolioPdf());
        }
        assertEquals(4, requests.size());
        assertEquals(1, saves.get());
        JsonNode first = requests.get(0);
        JsonNode content = first.path("input").get(0).path("content");
        assertEquals("user", first.path("input").get(0).path("role").asText());
        assertEquals(2, content.size());
        assertEquals("input_text", content.get(0).path("type").asText());
        JsonNode input = mapper.readTree(content.get(0).path("text").asText());
        assertTrue(input.path("hasPortfolio").asBoolean());
        assertTrue(input.path("documentText").asText().contains("서비스 프로젝트"));
        assertFalse(input.toString().contains("private/path"));
        assertFalse(input.has("portfolioPdf"));
        JsonNode file = content.get(1);
        assertEquals("input_file", file.path("type").asText());
        assertEquals("portfolio.pdf", file.path("filename").asText());
        assertEquals("high", file.path("detail").asText());
        String prefix = "data:application/pdf;base64,";
        assertTrue(file.path("file_data").asText().startsWith(prefix));
        assertArrayEquals(pdf, Base64.getDecoder().decode(file.path("file_data").asText().substring(prefix.length())));
        assertTrue(first.path("instructions").asText().contains("포트폴리오 질문은 반드시 2번 하나만"));
        assertTrue(first.path("instructions").asText().contains("첨부 PDF의 텍스트·이미지"));
        for (int index = 0; index < requests.size(); index++) {
            assertEquals("gpt-6-sol", requests.get(index).path("model").asText());
            assertEquals("medium", requests.get(index).path("reasoning").path("effort").asText());
            assertFalse(requests.get(index).path("store").asBoolean());
            if (index == 1 || index == 2) {
                assertTrue(requests.get(index).path("input").isTextual());
                assertFalse(requests.get(index).path("input").asText().contains("base64"));
                assertFalse(requests.get(index).path("input").asText().contains("documentText"));
            }
        }
        JsonNode evaluationContent = requests.get(3).path("input").get(0).path("content");
        assertEquals(file, evaluationContent.get(1));
        JsonNode evaluationInput = mapper.readTree(evaluationContent.get(0).path("text").asText());
        assertEquals(input.path("documentText"), evaluationInput.path("documentText"));
        assertTrue(evaluationInput.path("hasPortfolio").asBoolean());
        assertEquals(5, evaluationInput.path("questionAnswers").size());
        assertFalse(evaluationInput.toString().contains("private/path"));
    }

    @Test
    public void failedEvaluationReleasesPortfolioSnapshotWithoutRetry() throws Exception {
        LlmInterviewContextDTO[] prepared = new LlmInterviewContextDTO[1];
        var builder = new InterviewDocumentInputBuilder(storage(portfolioPdf())) {
            @Override
            public void prepare(LlmInterviewContextDTO context) {
                super.prepare(context);
                prepared[0] = context;
            }
        };
        var saves = new AtomicInteger();
        var dao = dao(saves, portfolio());
        var service = new InterviewServiceImpl(dao, generator("sk-local-test-only"),
                new InterviewPersistenceService(dao), builder, testClock());
        var settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setPortfolioNum(3);
        settings.setInterviewDifficulty("NORMAL");
        int historyNum = service.startInterview(7, settings).getHistoryNum();
        for (int order = 1; order <= 4; order++) {
            var answer = new InterviewAnswerRequestDTO();
            answer.setQuestionOrder(order);
            answer.setAnswer("답변 " + order);
            service.submitAnswer(7, historyNum, answer);
        }
        assertNotNull(prepared[0].getPortfolioPdf());
        overrideResponse = "{\"status\":\"incomplete\",\"output\":[]}";
        var answer = new InterviewAnswerRequestDTO();
        answer.setQuestionOrder(5);
        answer.setAnswer("마지막 답변");
        assertThrows(InterviewProcessingException.class, () -> service.submitAnswer(7, historyNum, answer));
        assertNull(prepared[0].getPortfolioPdf());
        assertThrows(InterviewProcessingException.class, () -> service.submitAnswer(7, historyNum, answer));
        assertEquals(4, requests.size());
        assertEquals(0, saves.get());
    }

    @Test
    public void portfolioQuestionMustAppearOnlyAtSecondPosition() throws Exception {
        for (int position = 0; position < 3; position++) {
            ObjectNode generated = mapper.createObjectNode();
            var questions = generated.putArray("questions");
            for (int index = 0; index < 3; index++) {
                questions.add(index == position ? "포트폴리오의 고객 분석에서 본인의 역할은 무엇인가요?" : "판단 근거는 무엇인가요?");
            }
            overrideResponse = documentResponse(generated);
            var request = documentRequest();
            request.getContext().setPortfolio(portfolio());
            request.getContext().setPortfolioPdf(portfolioPdf());
            if (position == 1) {
                var result = generator("sk-local-test-only").generateDocumentQuestions(request);
                assertTrue(result.getQuestions().get(1).getQuestionText().contains("포트폴리오"));
                assertEquals(3, result.getQuestions().size());
            } else {
                assertThrows(InterviewProcessingException.class,
                        () -> generator("sk-local-test-only").generateDocumentQuestions(request));
            }
            assertEquals(position + 1, requests.size());
        }
    }

    @Test
    public void duplicatePortfolioQuestionsAreRejectedWithoutRetry() throws Exception {
        ObjectNode generated = mapper.createObjectNode();
        generated.putArray("questions").add("포트폴리오에서 본인의 역할은 무엇인가요?")
                .add("포트폴리오의 판단 근거는 무엇인가요?").add("고객 세분화의 원리는 무엇인가요?");
        overrideResponse = documentResponse(generated);
        var request = documentRequest();
        request.getContext().setPortfolio(portfolio());
        request.getContext().setPortfolioPdf(portfolioPdf());
        assertThrows(InterviewProcessingException.class,
                () -> generator("sk-local-test-only").generateDocumentQuestions(request));
        assertEquals(1, requests.size());
    }

    @Test
    public void portfolioQuestionWithoutSelectedPortfolioIsRejectedWithoutRetry() throws Exception {
        ObjectNode generated = mapper.createObjectNode();
        generated.putArray("questions").add("자기소개서에서 본인의 역할은 무엇인가요?")
                .add("포트폴리오의 판단 근거는 무엇인가요?").add("고객 세분화의 원리는 무엇인가요?");
        overrideResponse = documentResponse(generated);
        assertThrows(InterviewProcessingException.class,
                () -> generator("sk-local-test-only").generateDocumentQuestions(documentRequest()));
        assertEquals(1, requests.size());
    }

    @Test
    public void missingPortfolioQuestionIsRejectedWithoutAutomaticRetry() throws Exception {
        ObjectNode generated = mapper.createObjectNode();
        generated.putArray("questions").add("본인의 역할은 무엇인가요?")
                .add("판단 근거는 무엇인가요?").add("결과를 입증할 근거는 무엇인가요?");
        overrideResponse = documentResponse(generated);
        LlmInterviewContextDTO[] prepared = new LlmInterviewContextDTO[1];
        var builder = new InterviewDocumentInputBuilder(storage(portfolioPdf())) {
            @Override
            public void prepare(LlmInterviewContextDTO context) {
                super.prepare(context);
                prepared[0] = context;
            }
        };
        InterviewDAO dao = dao(new AtomicInteger(), portfolio());
        var service = new InterviewServiceImpl(dao, generator("sk-local-test-only"), new InterviewPersistenceService(dao), builder, testClock());
        var settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setPortfolioNum(3);
        settings.setInterviewDifficulty("NORMAL");
        settings.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        var exception = assertThrows(InterviewProcessingException.class, () -> service.startInterview(7, settings));
        assertTrue(exception.getMessage().contains("포트폴리오 관련 질문"));
        assertTrue(exception.isRestartRequired());
        assertNull(prepared[0].getPortfolioPdf());
        assertSame(exception, assertThrows(InterviewProcessingException.class, () -> service.startInterview(7, settings)));
        assertEquals(1, requests.size());
    }

    @Test
    public void selectedPortfolioWithoutPdfFailsBeforeAnyHttpCall() {
        var request = documentRequest();
        request.getContext().setPortfolio(portfolio());
        assertThrows(IllegalArgumentException.class, () -> generator("sk-local-test-only").generateDocumentQuestions(request));
        assertEquals(0, requests.size());
    }

    private String documentResponse(ObjectNode generated) throws Exception {
        ObjectNode response = mapper.createObjectNode().put("status", "completed");
        response.putArray("output").addObject().put("type", "message").putArray("content")
                .addObject().put("type", "output_text").put("text", mapper.writeValueAsString(generated));
        return mapper.writeValueAsString(response);
    }

    private byte[] portfolioPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private PortfolioDTO portfolio() {
        var portfolio = new PortfolioDTO();
        portfolio.setPortfolioTitle("서비스 프로젝트");
        portfolio.setFileUrl("private/path/portfolio.pdf");
        return portfolio;
    }

    private PortfolioFileStorage storage(byte[] pdf) {
        return new PortfolioFileStorage() {
            public String store(int portfolioNum, MultipartFile file) { throw new UnsupportedOperationException(); }
            public void deleteIfExists(String fileUrl) { throw new UnsupportedOperationException(); }
            public Resource loadAsResource(String fileUrl) { return new ByteArrayResource(pdf); }
        };
    }

    @Test
    public void compactInputsPreserveRawTextAndTimeoutEvidence() throws Exception {
        var generator = generator("sk-local-test-only");
        var initial = documentRequest();
        String document = "[이력서]\n본인 기여 30% · 고객 분석 😀\r\n[자기소개서] \"근거\"와 한계";
        initial.getContext().setDocumentText(document);
        generator.generateDocumentQuestions(initial);
        assertEquals(document, mapper.readTree(requests.get(0).path("input").asText()).path("documentText").asText());

        var follow = new LlmFollowUpRequestDTO();
        follow.setContext(initial.getContext());
        follow.setInterviewDifficulty("HARD");
        List<InterviewQuestionAnswerDTO> answers = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            var answer = new InterviewQuestionAnswerDTO();
            answer.setQuestionOrder(index);
            answer.setQuestionType("DOCUMENT");
            answer.setQuestion("질문 " + index + "\n성과 근거는 무엇인가요?");
            answer.setAnswer(index == 3 ? "" : "  답변 " + index + "\r\n\"비교 조건\" 😀  ");
            answer.setTimedOut(index == 3);
            answers.add(answer);
        }
        follow.setQuestionAnswers(answers);
        generator.generateFollowUpQuestion(4, follow);
        JsonNode input = mapper.readTree(requests.get(1).path("input").asText());
        assertFalse(input.has("styleGuide"));
        assertTrue(input.path("difficultyGuide").asText().startsWith("압박면접(HARD)"));
        for (int index = 0; index < 3; index++) {
            JsonNode item = input.path("questionAnswers").get(index);
            assertEquals(answers.get(index).getQuestion(), item.path("question").asText());
            assertEquals(answers.get(index).getAnswer(), item.path("answer").asText());
            assertEquals(answers.get(index).isTimedOut(), item.path("timedOut").asBoolean());
        }
        assertTrue(input.path("questionAnswers").get(2).path("timedOut").asBoolean());
        assertTrue(mapper.writeValueAsString(input.path("questionAnswers")).length()
                < mapper.writeValueAsString(answers).length());
        answers.get(0).setQuestionOrder(2);
        assertThrows(IllegalArgumentException.class, () -> generator.generateFollowUpQuestion(4, follow));
        assertEquals(2, requests.size());
    }

    @Test
    public void costEstimateCountsEachTokenCategoryOnce() {
        assertEquals(0, new java.math.BigDecimal("0.00257")
                .compareTo(OpenAiInterviewClient.estimateCost(1000, 600, 100, 160)));
        assertEquals(0, new java.math.BigDecimal("0.0036")
                .compareTo(OpenAiInterviewClient.estimateCost(1000, 0, 0, 160)));
    }

    @Test
    public void missingKeyFailsBeforeAnyHttpCall() {
        var exception = assertThrows(InterviewProcessingException.class,
                () -> generator("").generateDocumentQuestions(documentRequest()));
        assertFalse(exception.isRestartRequired());
        assertEquals(0, requests.size());
    }

    @Test
    public void corruptedKeyFailsBeforeAnyHttpCall() {
        var exception = assertThrows(InterviewProcessingException.class,
                () -> generator("sk-local-test-only\uFFFD\uFFFD").generateDocumentQuestions(documentRequest()));
        assertTrue(exception.getMessage().contains("잘못된 문자"));
        assertEquals(0, requests.size());
    }

    @Test
    public void oversizedDocumentFailsBeforeAnyHttpCall() {
        var request = documentRequest();
        request.getContext().setDocumentText("가".repeat(9001));
        assertThrows(IllegalArgumentException.class, () -> generator("sk-local-test-only").generateDocumentQuestions(request));
        assertEquals(0, requests.size());
    }

    @Test
    public void rateLimitDoesNotRetryOrReturnDummyQuestions() {
        status = 429;
        overrideResponse = "{\"error\":{\"message\":\"private provider message\"}}";
        var exception = assertThrows(InterviewProcessingException.class,
                () -> generator("sk-local-test-only").generateDocumentQuestions(documentRequest()));
        assertTrue(exception.isRestartRequired());
        assertFalse(exception.getMessage().contains("private"));
        assertEquals(1, requests.size());
    }

    @Test
    public void incompleteRefusalAndMissingFieldsAreRejectedWithoutRetries() {
        String[] invalid = {
            "{\"status\":\"incomplete\",\"output\":[]}",
            "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}",
            "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{}\"}]}]}"
        };
        for (int index = 0; index < invalid.length; index++) {
            overrideResponse = invalid[index];
            assertThrows(InterviewProcessingException.class,
                    () -> generator("sk-local-test-only").generateDocumentQuestions(documentRequest()));
            assertEquals(index + 1, requests.size());
        }
    }

    private OpenAiInterviewGenerator generator(String key) {
        return new OpenAiInterviewGenerator(new OpenAiInterviewClient(key, endpoint, Duration.ofSeconds(5)));
    }

    private LlmQuestionRequestDTO documentRequest() {
        var request = new LlmQuestionRequestDTO();
        var context = new LlmInterviewContextDTO();
        context.setDocumentText("[이력서] 마케팅 인턴 [자기소개서] 고객 분석 경험");
        request.setContext(context);
        request.setInterviewDifficulty("NORMAL");
        return request;
    }

    private Clock testClock() {
        return Clock.fixed(Instant.parse("2026-09-28T03:00:00Z"), ZoneOffset.UTC);
    }

    private InterviewDAO dao(AtomicInteger saves) {
        return dao(saves, null);
    }

    private InterviewDAO dao(AtomicInteger saves, PortfolioDTO portfolio) {
        ResumeDTO resume = new ResumeDTO();
        resume.setResumeTitle("마케팅 지원 이력서");
        resume.setOccupationCode("14");
        resume.setOccupationName("마케팅·홍보·조사");
        resume.setJobCode("310");
        resume.setJobName("콘텐츠마케팅");
        CoverLetterDTO letter = new CoverLetterDTO();
        letter.setProblemSolvingExperience("고객 설문을 분석해 콘텐츠 변경");
        InterviewResultDTO previous = new InterviewResultDTO();
        previous.setTotalScore(60);
        previous.setWeaknesses("성과 근거 부족");
        return (InterviewDAO) Proxy.newProxyInstance(InterviewDAO.class.getClassLoader(),
                new Class<?>[] {InterviewDAO.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "selectResumeByResumeNumAndUserNum" -> resume;
                    case "selectUserJobPreferenceByUserNum" -> throw new AssertionError(
                            "AI 면접의 지원 분야는 선택한 이력서에서 읽어야 합니다.");
                    case "selectCoverLetterByLetterNumAndUserNum" -> letter;
                    case "selectPortfolioByPortfolioNumAndUserNum" -> portfolio;
                    case "selectEducationListByResumeNum", "selectCareerListByResumeNum", "selectCertificationListByResumeNum" -> List.of();
                    case "selectNextHistoryNum" -> 17;
                    case "selectLatestInterviewResultByUserNum" -> previous;
                    case "insertInterviewHistory" -> saves.incrementAndGet();
                    case "insertInterviewResult" -> 1;
                    default -> null;
                });
    }

}
