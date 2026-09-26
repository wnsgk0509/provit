package com.provit.service.interview.generator;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.provit.service.interview.InterviewProcessingException;

@Component
public class OpenAiInterviewClient {
    public static final String MODEL = "gpt-6-sol";
    private static final Logger log = LoggerFactory.getLogger(OpenAiInterviewClient.class);
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final HttpClient http;
    private final URI endpoint;
    private final Duration timeout;
    private final String apiKey;

    @Autowired
    public OpenAiInterviewClient(@Value("${api.openai.key:}") String apiKey) {
        this(apiKey, URI.create("https://api.openai.com/v1/responses"), Duration.ofSeconds(120));
    }

    // Local HTTP fixture tests use the same transport and response parser.
    OpenAiInterviewClient(String apiKey, URI endpoint, Duration timeout) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public JsonNode generate(String stage, String instructions, ObjectNode input,
            ObjectNode schema, int maxOutputTokens) {
        if (!apiKey.startsWith("sk-") || apiKey.chars().anyMatch(Character::isWhitespace)
                || apiKey.contains("입력") || apiKey.contains("발급")) {
            throw new InterviewProcessingException(
                    "OpenAI API 키가 설정되지 않았습니다. 관리자에게 문의해 주세요.", false, false);
        }
        if (!apiKey.matches("sk-[A-Za-z0-9_-]+")) {
            throw new InterviewProcessingException(
                    "OpenAI API 키에 잘못된 문자가 포함되어 있습니다. 따옴표나 깨진 문자를 제거해 주세요.", false, false);
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", MODEL);
            body.put("instructions", instructions);
            body.put("input", mapper.writeValueAsString(input));
            body.putObject("reasoning").put("effort", "medium");
            body.put("max_output_tokens", maxOutputTokens);
            body.put("store", false);
            ObjectNode format = body.putObject("text").putObject("format");
            format.put("type", "json_schema");
            format.put("name", "interview_" + stage);
            format.put("strict", true);
            format.set("schema", schema);
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            // No automatic application retries: each stage consumes at most one generation request.
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("Interview OpenAI stage={} HTTP status={}", stage, response.statusCode());
                throw failure(response.statusCode() == 401
                        ? "OpenAI API 키 인증에 실패했습니다. 키 값과 배포된 설정을 확인해 주세요."
                        : response.statusCode() == 403
                            ? "OpenAI API 요청이 거부되었습니다. 프로젝트 권한과 접속 지역 설정을 확인해 주세요."
                        : response.statusCode() == 404
                            ? "지정한 OpenAI 모델에 접근할 수 없습니다. 프로젝트의 모델 접근 권한을 확인해 주세요."
                        : response.statusCode() == 429
                            ? "AI 서비스의 사용 한도에 도달했습니다. 잠시 후 새 면접을 시작해 주세요."
                            : "AI 서비스 요청에 실패했습니다. 새 면접을 시작해 주세요.");
            }
            JsonNode root = mapper.readTree(response.body());
            logUsage(stage, root.path("usage"));
            if (!"completed".equals(root.path("status").asText())) {
                throw failure("AI 응답이 완성되지 않았습니다. 새 면접을 시작해 주세요.");
            }
            StringBuilder output = new StringBuilder();
            for (JsonNode item : root.path("output")) {
                if (!"message".equals(item.path("type").asText())) continue;
                for (JsonNode content : item.path("content")) {
                    if ("refusal".equals(content.path("type").asText())) {
                        throw failure("AI가 해당 자료의 면접 처리를 완료하지 못했습니다. 새 면접을 시작해 주세요.");
                    }
                    if ("output_text".equals(content.path("type").asText())) {
                        output.append(content.path("text").asText());
                    }
                }
            }
            if (output.isEmpty()) throw failure("AI 응답에 면접 결과가 없습니다. 새 면접을 시작해 주세요.");
            JsonNode generated = mapper.readTree(output.toString());
            validate(generated, schema);
            return generated;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure("AI 요청이 중단되었습니다. 새 면접을 시작해 주세요.");
        } catch (IOException exception) {
            // Never log authorization, documents, answers, or upstream response bodies.
            log.warn("Interview OpenAI stage={} transport/JSON failure={}", stage,
                    exception.getClass().getSimpleName());
            throw failure("AI 응답을 받지 못했습니다. 새 면접을 시작해 주세요.");
        }
    }

    private void logUsage(String stage, JsonNode usage) {
        if (!usage.has("input_tokens") || !usage.has("output_tokens")) return;
        long input = usage.path("input_tokens").asLong();
        long output = usage.path("output_tokens").asLong();
        long cached = usage.path("input_tokens_details").path("cached_tokens").asLong();
        BigDecimal estimated = BigDecimal.valueOf(input - cached).multiply(new BigDecimal("0.000002"))
                .add(BigDecimal.valueOf(cached).multiply(new BigDecimal("0.0000002")))
                .add(BigDecimal.valueOf(output).multiply(new BigDecimal("0.00001")));
        log.info("Interview OpenAI stage={} model={} inputTokens={} cachedTokens={} outputTokens={} estimatedUSD={}",
                stage, MODEL, input, cached, output, estimated.toPlainString());
    }

    private void validate(JsonNode value, JsonNode schema) {
        String type = schema.path("type").asText();
        if (value == null || value.isNull()) throw invalidOutput();
        if ("object".equals(type)) {
            if (!value.isObject()) throw invalidOutput();
            JsonNode properties = schema.path("properties");
            for (JsonNode field : schema.path("required")) {
                if (!value.has(field.asText())) throw invalidOutput();
            }
            Iterator<String> fields = value.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!properties.has(field)) throw invalidOutput();
                validate(value.get(field), properties.get(field));
            }
        } else if ("array".equals(type)) {
            if (!value.isArray() || value.size() < schema.path("minItems").asInt(0)
                    || value.size() > schema.path("maxItems").asInt(Integer.MAX_VALUE)) throw invalidOutput();
            for (JsonNode item : value) validate(item, schema.path("items"));
        } else if ("string".equals(type)) {
            if (!value.isTextual() || value.asText().isBlank()
                    || value.asText().length() > schema.path("maxLength").asInt(Integer.MAX_VALUE)) throw invalidOutput();
        } else if ("integer".equals(type) || "number".equals(type)) {
            if (!value.isNumber() || ("integer".equals(type) && !value.isIntegralNumber())
                    || !Double.isFinite(value.asDouble())
                    || value.asDouble() < schema.path("minimum").asDouble(-Double.MAX_VALUE)
                    || value.asDouble() > schema.path("maximum").asDouble(Double.MAX_VALUE)) throw invalidOutput();
        }
        if (schema.has("enum")) {
            boolean matches = false;
            for (JsonNode option : schema.get("enum")) matches |= option.equals(value);
            if (!matches) throw invalidOutput();
        }
    }

    private InterviewProcessingException invalidOutput() {
        return failure("AI 응답 형식이 올바르지 않습니다. 새 면접을 시작해 주세요.");
    }

    private InterviewProcessingException failure(String message) {
        return new InterviewProcessingException(message, true, false);
    }
}
