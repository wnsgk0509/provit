package com.provit.service.interview.generator;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import com.provit.dao.interview.InterviewDAO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.interview.*;
import com.provit.service.interview.InterviewDocumentInputBuilder;
import com.provit.service.interview.InterviewPersistenceService;
import com.provit.service.interview.InterviewProcessingException;
import com.provit.service.interview.impl.InterviewServiceImpl;

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
                case "interview_document" -> "{\"questions\":[" + question(1, "DOCUMENT") + ","
                        + question(2, "DOCUMENT") + "," + question(3, "DOCUMENT") + "]}";
                case "interview_follow_up_4" -> question(4, "FOLLOW_UP");
                case "interview_follow_up_5" -> question(5, "FOLLOW_UP");
                default -> "{\"confidenceScore\":70,\"persistenceScore\":75,\"expertiseScore\":80,"
                        + "\"logicScore\":85,\"deliveryScore\":90,\"strengths\":\"1번에서 기여를 설명했습니다.\","
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
        AtomicInteger saves = new AtomicInteger();
        InterviewDAO dao = dao(saves);
        var service = new InterviewServiceImpl(dao, generator("sk-local-test-only"),
                new InterviewPersistenceService(dao), new InterviewDocumentInputBuilder(null));
        InterviewStartRequestDTO settings = new InterviewStartRequestDTO();
        settings.setResumeNum(1);
        settings.setLetterNum(2);
        settings.setInterviewStyle("ONE_TO_ONE");
        settings.setInterviewDifficulty("NORMAL");
        settings.setRequestId("feea354e-64fe-4fb1-af81-05cf18f21c43");
        var started = service.startInterview(7, settings);
        assertSame(started, service.startInterview(7, settings));
        assertEquals(1, requests.size());
        for (int order = 1; order <= 5; order++) {
            var answer = new InterviewAnswerRequestDTO();
            answer.setQuestionOrder(order);
            answer.setAnswer("답변 " + order);
            var response = service.submitAnswer(7, started.getHistoryNum(), answer);
            assertSame(response, service.submitAnswer(7, started.getHistoryNum(), answer));
            if (order == 5) assertEquals(80.0, response.getResult().getTotalScore(), 0.001);
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
            assertFalse(input.has("userNum"));
            if (index == 0) {
                assertTrue(input.path("documentText").asText().contains("[자기소개서]"));
            } else {
                assertFalse(input.has("documentText"));
                assertFalse(input.has("context"));
                assertEquals(index + 2, input.path("questionAnswers").size());
            }
            if (index == 3) {
                assertEquals(60.0, input.path("previousResult").path("totalScore").asDouble(), 0.001);
                assertEquals("성과 근거 부족", input.path("previousResult").path("weaknesses").asText());
                assertFalse(input.path("previousResult").has("userNum"));
            }
        }
        assertTrue(requests.get(1).path("instructions").asText().contains("전체를 검토"));
        assertTrue(requests.get(2).path("instructions").asText().contains("4번 답변을 중심"));
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
        request.setInterviewStyle("ONE_TO_ONE");
        request.setInterviewDifficulty("NORMAL");
        return request;
    }

    private InterviewDAO dao(AtomicInteger saves) {
        ResumeDTO resume = new ResumeDTO();
        resume.setResumeTitle("마케팅 지원 이력서");
        CoverLetterDTO letter = new CoverLetterDTO();
        letter.setProblemSolvingExperience("고객 설문을 분석해 콘텐츠 변경");
        InterviewResultDTO previous = new InterviewResultDTO();
        previous.setTotalScore(60);
        previous.setWeaknesses("성과 근거 부족");
        return (InterviewDAO) Proxy.newProxyInstance(InterviewDAO.class.getClassLoader(),
                new Class<?>[] {InterviewDAO.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "selectResumeByResumeNumAndUserNum" -> resume;
                    case "selectCoverLetterByLetterNumAndUserNum" -> letter;
                    case "selectEducationListByResumeNum", "selectCareerListByResumeNum", "selectCertificationListByResumeNum" -> List.of();
                    case "selectNextHistoryNum" -> 17;
                    case "selectLatestInterviewResultByUserNum" -> previous;
                    case "insertInterviewHistory" -> saves.incrementAndGet();
                    case "insertInterviewResult" -> 1;
                    default -> null;
                });
    }

    private String question(int order, String type) {
        return "{\"questionOrder\":" + order + ",\"questionType\":\"" + type
                + "\",\"questionText\":\"고객 분석에서 본인의 판단을 설명해 주세요.\"}";
    }
}
