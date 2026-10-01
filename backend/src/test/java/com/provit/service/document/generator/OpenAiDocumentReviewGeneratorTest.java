package com.provit.service.document.generator;

import static org.junit.Assert.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO.Document;
import com.provit.service.document.DocumentReviewProcessingException;

public class OpenAiDocumentReviewGeneratorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger calls = new AtomicInteger();
    private HttpServer server;
    private ObjectNode sent;
    private ObjectNode expected;
    private List<Document> documents;
    private DocumentReviewRequestDTO request;
    private String reply;
    private int status = 200;
    private long responseDelay;
    private OpenAiDocumentReviewGenerator generator;

    @Before
    public void setUp() throws Exception {
        expected = (ObjectNode) mapper.readTree(Files.readString(Path.of("docs/examples/document-review/response.example.json")));
        var input = mapper.readTree(Files.readString(Path.of("docs/examples/document-review/input.example.json")));
        documents = new ArrayList<>();
        var resume = mapper.createObjectNode();
        var info = ((ObjectNode) input.path("documents").path("resume")).deepCopy();
        for (String name : List.of("educationList", "careerList", "certificationList")) resume.set(name, info.remove(name));
        info.put("userNum", 7).put("resumeNum", 11);
        resume.set("resume", info);
        documents.add(snapshot("resume", info.path("title").asText(), resume));
        var letter = ((ObjectNode) input.path("documents").path("coverLetter")).deepCopy().put("userNum", 7).put("letterNum", 12);
        documents.add(snapshot("cover-letter", letter.path("title").asText(), letter));
        request = new DocumentReviewRequestDTO();
        request.setResumeNum(11); request.setLetterNum(12); request.setReviewMode("comprehensive");
        request.setCustomCriteria("unused draft"); request.setInstructions("  실제 경험만 사용  ");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            calls.incrementAndGet();
            sent = (ObjectNode) mapper.readTree(exchange.getRequestBody().readAllBytes());
            if (responseDelay > 0) {
                try { Thread.sleep(responseDelay); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
            byte[] body = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
        server.start();
        generator = new OpenAiDocumentReviewGenerator("test-key", URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses"), Duration.ofSeconds(3));
        completed(expected);
    }

    @After
    public void tearDown() { if (server != null) server.stop(0); }

    @Test
    public void sendsOneFixedModelRequestAndParsesTextAfterReasoningItem() throws Exception {
        var result = generator.generate(request, documents);
        assertEquals(1, calls.get()); assertEquals(expected.path("summary").asText(), result.getSummary());
        assertEquals("gpt-6.1-sol", sent.path("model").asText());
        assertEquals("medium", sent.path("reasoning").path("effort").asText());
        assertFalse(sent.path("stream").asBoolean()); assertFalse(sent.path("store").asBoolean());
        assertEquals(25000, sent.path("max_output_tokens").asInt());
        assertTrue(sent.path("text").path("format").path("strict").asBoolean());
        assertEquals(1, sent.path("input").get(1).path("content").size());
        var input = mapper.readTree(sent.path("input").get(1).path("content").get(0).path("text").asText());
        assertTrue(input.path("customCriteria").isNull()); assertEquals("실제 경험만 사용", input.path("instructions").asText());
        assertFalse(input.toString().contains("userNum")); assertFalse(input.toString().contains("resumeNum"));
        assertEquals("null", sent.path("text").path("format").path("schema").path("properties")
                .path("documentReviews").path("properties").path("portfolio").path("type").asText());
        assertEquals(3, result.getCareerPreparation().getRecommendations().size());
    }

    @Test
    public void acceptsCurrentModelSnapshotAndRejectsPreviousOrMalformedModel() throws Exception {
        var envelope = (ObjectNode) mapper.readTree(reply);
        envelope.put("model", "gpt-6.1-sol-2026-09-29");
        reply = mapper.writeValueAsString(envelope);
        assertEquals(expected.path("summary").asText(), generator.generate(request, documents).getSummary());
        for (String model : List.of("gpt-6-sol", "gpt-6x1-sol-2026-09-29")) {
            envelope.put("model", model);
            reply = mapper.writeValueAsString(envelope);
            assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        }
    }

    @Test
    public void attachesPdfInSameRequestAndValidatesPageRange() throws Exception {
        byte[] bytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new java.io.ByteArrayOutputStream()) {
            pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage()); pdf.save(output); bytes = output.toByteArray();
        }
        var portfolio = snapshot("portfolio", "프로젝트 PDF", mapper.createObjectNode().put("fileUrl", "private-path"));
        portfolio.setOriginalFileName("project.pdf"); portfolio.setPdfSnapshot(bytes); documents.add(portfolio);
        ((ObjectNode) expected.path("documentReviews")).set("portfolio", expected.path("documentReviews").path("resume").deepCopy());
        completed(expected);
        var result = generator.generate(request, documents);
        assertNotNull(result.getDocumentReviews().get("portfolio")); assertEquals(1, calls.get());
        var file = sent.path("input").get(1).path("content").get(1);
        assertEquals("input_file", file.path("type").asText()); assertEquals("high", file.path("detail").asText());
        assertArrayEquals(bytes, java.util.Base64.getDecoder().decode(file.path("file_data").asText().split(",", 2)[1]));
        assertFalse(sent.toString().contains("private-path"));
        ((ObjectNode) expected.path("consistencyIssues").get(0).path("sources").get(0))
                .put("documentType", "portfolio").put("pageNumber", 2);
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(2, calls.get());
    }

    @Test
    public void invalidPdfDoesNotCallApi() {
        var pdf = snapshot("portfolio", "broken", mapper.createObjectNode());
        pdf.setPdfSnapshot("not pdf".getBytes(StandardCharsets.UTF_8)); documents.add(pdf);
        assertEquals(400, assertThrows(DocumentReviewProcessingException.class,
                () -> generator.generate(request, documents)).getHttpStatus());
        assertEquals(0, calls.get());
    }

    @Test
    public void rejectsInventedOriginalWithoutRepairCall() throws Exception {
        ((ObjectNode) expected.path("documentReviews").path("resume").path("improvements").get(0)).put("original", "없는 원문");
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(1, calls.get());
    }

    @Test
    public void rejectsChangedJobOrUnselectedDocument() throws Exception {
        ((ObjectNode) expected.path("careerPreparation")).put("jobName", "없는 직무");
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        ((ObjectNode) expected.path("careerPreparation")).put("jobName", "백엔드 개발자");
        ((ObjectNode) expected.path("consistencyIssues").get(0).path("sources").get(0)).put("documentType", "portfolio");
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(2, calls.get());
    }

    @Test
    public void rejectsExtraFieldsExcessLengthAndMalformedJson() throws Exception {
        expected.put("extra", "private"); completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        expected.remove("extra"); expected.put("summary", "가".repeat(501)); completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        reply = "{invalid";
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(3, calls.get());
    }

    @Test
    public void rejectsHttpErrorRefusalAndIncompleteWithoutRetries() throws Exception {
        status = 429; reply = "private upstream error";
        assertEquals(503, assertThrows(DocumentReviewProcessingException.class,
                () -> generator.generate(request, documents)).getHttpStatus());
        status = 200;
        reply = "{\"model\":\"gpt-6.1-sol\",\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"private\"}]}]}";
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        reply = "{\"model\":\"gpt-6.1-sol\",\"status\":\"incomplete\",\"output\":[]}";
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(3, calls.get());
    }

    @Test
    public void missingKeyDoesNotCallApi() {
        var missing = new OpenAiDocumentReviewGenerator("");
        assertEquals(503, assertThrows(DocumentReviewProcessingException.class,
                () -> missing.generate(request, documents)).getHttpStatus());
        assertEquals(0, calls.get());
    }

    @Test
    public void timeoutDoesNotRetry() {
        responseDelay = 700;
        var timed = new OpenAiDocumentReviewGenerator("test-key", URI.create("http://127.0.0.1:"
                + server.getAddress().getPort() + "/v1/responses"), Duration.ofMillis(300));
        assertEquals(504, assertThrows(DocumentReviewProcessingException.class,
                () -> timed.generate(request, documents)).getHttpStatus());
        assertEquals(1, calls.get());
    }

    @Test
    public void returnsStrengthQuoteReasonAndRewriteForOverallAndDocument() throws Exception {
        var strength = strengthExample();
        addStrengthEvidence(strength);
        expected.withArray("strengths").add(strength);
        ((ObjectNode) expected.path("documentReviews").path("coverLetter")).withArray("strengths").add(strength.deepCopy());
        completed(expected);
        var result = generator.generate(request, documents);
        var saved = result.getStrengths().get(0);
        assertEquals(strength.path("title").asText(), saved.getTitle());
        assertEquals(strength.path("reason").asText(), saved.getReason());
        assertEquals(strength.path("suggestion").asText(), saved.getSuggestion());
        assertEquals(strength.path("sources").get(0).path("text").asText(), saved.getSources().get(0).getText());
        assertEquals(saved, result.getDocumentReviews().get("coverLetter").getStrengths().get(0));
        assertFalse(mapper.writeValueAsString(saved).contains("reviewNum"));
        assertEquals(1, calls.get());
    }

    @Test
    public void rejectsStrengthWithoutEvidenceReasonOrRewriteAndInventedQuote() throws Exception {
        var strength = strengthExample();
        addStrengthEvidence(strength);
        for (String missing : List.of("reason", "suggestion", "sources")) {
            var bad = strength.deepCopy(); bad.remove(missing);
            expected.putArray("strengths").add(bad); completed(expected);
            assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        }
        var bad = strength.deepCopy(); bad.putArray("sources");
        expected.putArray("strengths").add(bad); completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        bad = strength.deepCopy(); ((ObjectNode) bad.path("sources").get(0)).put("text", "없는 근거 원문");
        expected.putArray("strengths").add(bad); completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(5, calls.get());
    }

    @Test
    public void documentStrengthCannotCiteAnotherDocumentAndPreparationQuoteIsVerified() throws Exception {
        var strength = strengthExample(); addStrengthEvidence(strength);
        ((ObjectNode) expected.path("documentReviews").path("resume")).withArray("strengths").add(strength);
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        ((ObjectNode) expected.path("documentReviews").path("resume")).putArray("strengths");
        ((ObjectNode) expected.path("careerPreparation").path("recommendations").get(0).path("sources").get(0))
                .put("text", "존재하지 않는 추천 근거");
        completed(expected);
        assertThrows(DocumentReviewProcessingException.class, () -> generator.generate(request, documents));
        assertEquals(2, calls.get());
    }

    private ObjectNode strengthExample() throws Exception {
        return (ObjectNode) mapper.readTree(Files.readString(Path.of("docs/examples/document-review/strength.example.json")));
    }

    private void addStrengthEvidence(ObjectNode strength) throws Exception {
        var letter = (ObjectNode) mapper.readTree(documents.get(1).getSourceSnapshotJson());
        letter.put("problemSolvingExperience", letter.path("problemSolvingExperience").asText() + "\n"
                + strength.path("sources").get(0).path("text").asText());
        documents.get(1).setSourceSnapshotJson(letter.toString());
    }

    private void completed(ObjectNode output) throws Exception {
        var envelope = mapper.createObjectNode().put("model", "gpt-6.1-sol").put("status", "completed");
        var items = envelope.putArray("output"); items.addObject().put("type", "reasoning");
        items.addObject().put("type", "message").putArray("content").addObject()
                .put("type", "output_text").put("text", mapper.writeValueAsString(output));
        reply = mapper.writeValueAsString(envelope);
    }

    private Document snapshot(String type, String title, com.fasterxml.jackson.databind.JsonNode value) {
        var document = new Document(); document.setDocumentType(type); document.setDocumentTitle(title);
        document.setSourceSnapshotJson(value.toString()); return document;
    }
}
