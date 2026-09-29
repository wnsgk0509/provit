package com.provit.service.document.generator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.document.DocumentReviewResultDTO.Document;
import com.provit.service.document.DocumentReviewProcessingException;

@Component
public class OpenAiDocumentReviewGenerator implements DocumentReviewGenerator {
    public static final String MODEL = "gpt-6-sol";
    public static final String PROMPT_VERSION = "document-review-v4";
    public static final int RESPONSE_VERSION = 3;
    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
            DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final String apiKey;
    private final URI endpoint;
    private final Duration timeout;
    private final String prompt;
    private final JsonNode schema;

    @Autowired
    public OpenAiDocumentReviewGenerator(@Value("${api.openai.key:}") String apiKey) {
        this(apiKey, URI.create("https://api.openai.com/v1/responses"), Duration.ofSeconds(120));
    }

    OpenAiDocumentReviewGenerator(String apiKey, URI endpoint, Duration timeout) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.endpoint = endpoint;
        this.timeout = timeout;
        try {
            prompt = resource("/document-review/prompt.txt");
            schema = mapper.readTree(resource("/document-review/response-schema.json"));
        } catch (IOException exception) { throw new IllegalStateException("첨삭 프롬프트를 읽지 못했습니다.", exception); }
    }

    @Override
    public DocumentReviewResultDTO generate(DocumentReviewRequestDTO request, List<Document> documents) {
        if (apiKey.isBlank()) throw failure("OpenAI API 키가 설정되지 않았습니다.", 503);
        try {
            ObjectNode input = normalizedInput(request, documents);
            ObjectNode responseSchema = schema.deepCopy();
            Document portfolio = documents.stream().filter(document -> "portfolio".equals(document.getDocumentType()))
                    .findFirst().orElse(null);
            int pages = 0;
            if (portfolio != null) {
                try (var pdf = Loader.loadPDF(portfolio.getPdfSnapshot())) {
                    if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1)
                        throw failure("포트폴리오는 암호가 없는 정상 PDF여야 합니다.", 400);
                    pages = pdf.getNumberOfPages();
                } catch (IOException exception) { throw failure("포트폴리오 PDF를 읽지 못했습니다. 파일을 확인해 주세요.", 400); }
            }
            var reviews = (ObjectNode) responseSchema.path("properties").path("documentReviews").path("properties");
            reviews.set("portfolio", portfolio == null ? mapper.createObjectNode().put("type", "null")
                    : mapper.createObjectNode().put("$ref", "#/$defs/feedback"));
            if (portfolio == null) {
                ((ObjectNode) responseSchema.path("$defs").path("source").path("properties").path("documentType"))
                        .set("enum", mapper.valueToTree(List.of("resume", "cover-letter")));
            }
            var body = mapper.createObjectNode();
            body.put("model", MODEL).put("store", false).put("stream", false).put("max_output_tokens", 25000);
            body.putObject("reasoning").put("effort", "medium");
            var format = body.putObject("text").putObject("format");
            format.put("type", "json_schema").put("name", "document_review_v3").put("strict", true).set("schema", responseSchema);
            var messages = body.putArray("input");
            messages.addObject().put("role", "developer").put("content", prompt);
            var content = messages.addObject().put("role", "user").putArray("content");
            content.addObject().put("type", "input_text").put("text", mapper.writeValueAsString(input));
            if (portfolio != null) {
                content.addObject().put("type", "input_file").put("filename", "portfolio.pdf").put("detail", "high")
                        .put("file_data", "data:application/pdf;base64," + Base64.getEncoder().encodeToString(portfolio.getPdfSnapshot()));
            }
            var httpRequest = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8)).build();
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw httpFailure(response.statusCode());
            var envelope = mapper.readTree(response.body());
            String responseModel = envelope.path("model").asText();
            if (!MODEL.equals(responseModel) && !responseModel.matches("gpt-6-sol-\\d{4}-\\d{2}-\\d{2}"))
                throw failure("요청한 AI 모델과 응답 모델이 다릅니다.", 502);
            if (!"completed".equals(envelope.path("status").asText()))
                throw failure("AI 응답이 완료되지 않았습니다. 첨삭 기록을 확인해 주세요.", 502);
            var output = new StringBuilder();
            for (JsonNode item : envelope.path("output")) {
                if (!"message".equals(item.path("type").asText())) continue;
                for (JsonNode part : item.path("content")) {
                    if ("refusal".equals(part.path("type").asText())) throw failure("AI가 첨삭 요청을 처리하지 못했습니다.", 502);
                    if ("output_text".equals(part.path("type").asText())) output.append(part.path("text").asText());
                }
            }
            if (output.length() == 0) throw failure("AI 첨삭 응답이 비어 있습니다.", 502);
            var result = mapper.readTree(output.toString());
            new DocumentReviewResponseValidator().validate(result, responseSchema, input, pages);
            return mapper.treeToValue(result, DocumentReviewResultDTO.class);
        } catch (DocumentReviewProcessingException exception) { throw exception; }
        catch (java.net.http.HttpTimeoutException exception) { throw failure("AI 응답 시간이 초과되었습니다. 첨삭 기록을 확인해 주세요.", 504); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure("첨삭 처리가 중단되었습니다.", 503);
        } catch (IOException exception) { throw failure("AI 통신 또는 응답 검증에 실패했습니다. 첨삭 기록을 확인해 주세요.", 502); }
    }

    ObjectNode normalizedInput(DocumentReviewRequestDTO request, List<Document> documents) throws IOException {
        var input = mapper.createObjectNode();
        input.put("reviewMode", request.getReviewMode());
        input.put("customCriteria", "custom".equals(request.getReviewMode()) ? optional(request.getCustomCriteria()) : null);
        input.put("instructions", optional(request.getInstructions()));
        var selected = input.putObject("documents");
        selected.putNull("portfolio");
        for (Document document : documents) {
            var snapshot = mapper.readTree(document.getSourceSnapshotJson());
            if ("resume".equals(document.getDocumentType())) {
                var resume = fields(snapshot.path("resume"), "occupationCode", "occupationName", "jobCode", "jobName",
                        "highestLevel", "educationName", "desiredLocation", "desiredWorkType", "motivation");
                resume.put("title", document.getDocumentTitle());
                list(resume, snapshot, "educationList", "schoolName", "admissionDate", "graduationDate", "major", "educationStatus");
                list(resume, snapshot, "careerList", "companyName", "joinDate", "resignDate", "mainDuty");
                list(resume, snapshot, "certificationList", "certName", "certGrade", "issueDate");
                selected.set("resume", resume);
            } else if ("cover-letter".equals(document.getDocumentType())) {
                var letter = fields(snapshot, "growthProcess", "personalityStrengthsWeaknesses", "problemSolvingExperience", "postJoiningAspiration");
                letter.put("title", document.getDocumentTitle());
                selected.set("coverLetter", letter);
            } else if ("portfolio".equals(document.getDocumentType())) {
                selected.putObject("portfolio").put("title", document.getDocumentTitle()).put("originalFilename", document.getOriginalFileName());
            }
        }
        return input;
    }

    private ObjectNode fields(JsonNode value, String... names) {
        var result = mapper.createObjectNode();
        for (String name : names) result.set(name, value.path(name).isMissingNode() ? mapper.nullNode() : value.path(name));
        return result;
    }

    private void list(ObjectNode target, JsonNode source, String name, String... names) {
        var array = target.putArray(name);
        for (JsonNode item : source.path(name)) array.add(fields(item, names));
    }

    private String optional(String text) { return text == null || text.isBlank() ? null : text.strip(); }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private DocumentReviewProcessingException httpFailure(int status) {
        if (status == 429) return failure("AI 요청 한도에 도달했습니다. 잠시 후 새 첨삭을 요청해 주세요.", 503);
        if (status == 401 || status == 403) return failure("OpenAI 인증 설정을 확인해야 합니다.", 503);
        if (status == 404) return failure("설정된 AI 모델을 사용할 수 없습니다. 서버 설정을 확인해 주세요.", 503);
        return failure("AI 서비스가 요청을 처리하지 못했습니다. 첨삭 기록을 확인해 주세요.", 502);
    }

    private DocumentReviewProcessingException failure(String message, int status) {
        return new DocumentReviewProcessingException(message, status);
    }
}
