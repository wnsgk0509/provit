package com.provit.client.fortune;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.client.fortune.dto.SazuTodayRequestDTO;

/**
 * sazu.app 만세력/사주 REST API 연동 클라이언트
 * 
 * - 엔드포인트: POST https://api.sazu.app/v2/sazu/today
 * - 인증: HTTP Header [X-API-Key: sazu_free_...]
 */
@Component
public class SazuApiClient {

    private static final Logger log = LoggerFactory.getLogger(SazuApiClient.class);
    private static final String DEFAULT_ENDPOINT = "https://api.sazu.app/v2/sazu/today";

    private final String apiKey;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper;

    @Autowired
    public SazuApiClient(@Value("${api.fortune.key:}") String apiKey) {
        this(apiKey, URI.create(DEFAULT_ENDPOINT), Duration.ofSeconds(15));
    }

    public SazuApiClient(String apiKey, URI endpoint, Duration timeout) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.endpoint = endpoint != null ? endpoint : URI.create(DEFAULT_ENDPOINT);
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(15);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * 생년월일 기준 오늘의 사주/운세 조회
     * 
     * @param request 생년월일/성별 요청 DTO
     * @return sazu.app 응답의 'data' JsonNode 객체
     */
    public JsonNode getTodayFortune(SazuTodayRequestDTO request) {
        if (apiKey.isEmpty() || apiKey.contains("발급") || apiKey.contains("입력")) {
            log.warn(">> [SazuApiClient] 사주 API 키가 설정되지 않았습니다. api.properties 확인 필요");
            throw new IllegalStateException("사주 API 키가 설정되지 않았습니다.");
        }

        SazuTodayRequestDTO targetRequest = request != null ? request : SazuTodayRequestDTO.defaultSample();

        // Free 샌드박스 키는 지정된 샘플 프로필만 허용하므로, Free 키인 경우 샘플 프로필로 안전하게 전환
        if (apiKey.startsWith("sazu_free_")) {
            log.info(">> [SazuApiClient] sazu_free 샌드박스 키 감지: 표준 테스트 샘플 프로필로 요청을 전송합니다.");
            targetRequest = SazuTodayRequestDTO.defaultSample();
        }

        try {
            String requestBody = mapper.writeValueAsString(targetRequest);
            log.info(">> [SazuApiClient] 사주 API 요청 전송: endpoint={}, body={}", endpoint, requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String responseBody = response.body();

            log.info(">> [SazuApiClient] 사주 API 응답 수신: statusCode={}", statusCode);

            if (statusCode != 200) {
                log.error(">> [SazuApiClient] 사주 API 호출 실패 (HTTP {}): {}", statusCode, responseBody);
                throw new RuntimeException("사주 API 호출에 실패했습니다. (HTTP " + statusCode + ")");
            }

            JsonNode rootNode = mapper.readTree(responseBody);
            boolean success = rootNode.path("success").asBoolean(false);

            if (!success) {
                String errorMsg = rootNode.path("error").path("message").asText("알 수 없는 사주 API 오류");
                log.error(">> [SazuApiClient] 사주 API 비즈니스 오류 응답: {}", errorMsg);
                throw new RuntimeException("사주 API 오류: " + errorMsg);
            }

            return rootNode.path("data");

        } catch (IOException e) {
            log.error(">> [SazuApiClient] 사주 API 입출력 또는 JSON 파싱 에러: {}", e.getMessage(), e);
            throw new RuntimeException("사주 API 데이터 처리 중 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(">> [SazuApiClient] 사주 API 요청 인터럽트: {}", e.getMessage(), e);
            throw new RuntimeException("사주 API 요청이 중단되었습니다.", e);
        }
    }

    /**
     * API 키 등록 및 유효성 여부 확인
     */
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !apiKey.contains("발급") && !apiKey.contains("입력");
    }
}
