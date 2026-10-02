package com.provit.client.fortune;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.provit.client.fortune.dto.SazuTodayRequestDTO;

public class SazuApiClientTest {

    private SazuApiClient client;
    private String apiKey;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("api.properties")) {
            if (is != null) {
                properties.load(is);
            }
        }
        apiKey = properties.getProperty("api.fortune.key", "");
        client = new SazuApiClient(apiKey);
    }

    @Test
    public void testGetTodayFortuneSuccess() {
        org.junit.Assume.assumeTrue(client.isConfigured());

        SazuTodayRequestDTO request = SazuTodayRequestDTO.defaultSample();
        JsonNode data = client.getTodayFortune(request);

        assertNotNull("응답 data 객체는 null이 아니어야 합니다.", data);
        assertEquals("토픽은 today여야 합니다.", "today", data.path("topic").asText());

        JsonNode modules = data.path("modules");
        assertTrue("modules 객체가 포함되어 있어야 합니다.", modules.isObject());
        assertTrue("오늘의 일운 dailyInteraction 정보가 포함되어 있어야 합니다.", modules.has("dailyInteraction"));
        assertTrue("사주 원국 fourPillars 정보가 포함되어 있어야 합니다.", modules.has("fourPillars"));

        JsonNode daily = modules.path("dailyInteraction");
        assertTrue("일주(ilju) 정보가 포함되어 있어야 합니다.", daily.has("ilju"));
        assertTrue("내 일간과의 관계(toDayMaster)가 포함되어 있어야 합니다.", daily.has("toDayMaster"));

        System.out.println("=================================================");
        System.out.println(">> [SazuApiClientTest] 사주 API 응답 검증 성공!");
        System.out.println(">> 기준 날짜: " + daily.path("date").asText());
        System.out.println(">> 오늘 일진 간지: " + daily.path("ilju").path("ganji").asText());
        System.out.println(">> 오늘의 십성 관계: " + daily.path("toDayMaster").path("stemSipseong").asText());
        System.out.println(">> 오늘의 요약: " + daily.path("summary").asText());
        System.out.println("=================================================");
    }
}
