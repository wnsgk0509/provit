package com.provit.controller.fortune;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.provit.dto.fortune.TodayFortuneDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.fortune.FortuneService;

public class FortuneControllerTest {

    private FortuneController fortuneController;
    private FortuneService fortuneService;

    @Before
    public void setUp() {
        // FortuneService Mock 생성
        fortuneService = (FortuneService) Proxy.newProxyInstance(
                FortuneService.class.getClassLoader(),
                new Class<?>[]{FortuneService.class},
                (proxy, method, args) -> {
                    if ("getTodayFortune".equals(method.getName())) {
                        Long userNum = (Long) args[0];
                        String nickname = (userNum != null) ? "테스트회원" : "취준생";
                        return TodayFortuneDTO.builder()
                                .fortuneDate("2026-10-02")
                                .userNickname(nickname)
                                .dayMaster("병화(병인)")
                                .dayMasterElement("화")
                                .todayIlju("경인일")
                                .todayElement("금 / 목")
                                .tenGodsRelation("편재")
                                .tenGodsMeaning("업계 트렌드와 기회 포착")
                                .overallScore(90)
                                .overallSummary("성장 잠재력 높은 기업을 발굴하기 좋은 날입니다.")
                                .advice("프로젝트 성과를 수치화하여 이력서에 반영하세요.")
                                .luckyJobName("데이터 엔지니어")
                                .luckyKeyword("정량적 데이터 분석")
                                .luckyColor("화이트 & 실버")
                                .luckyDirection("서쪽")
                                .luckyNumber(9)
                                .sinsalName("지살")
                                .sinsalAdvice("새로운 환경으로의 확장이 유리합니다.")
                                .recommendRecruitments(new ArrayList<RecruitmentDTO>())
                                .build();
                    }
                    return null;
                }
        );

        fortuneController = new FortuneController(fortuneService, null, null);
    }

    @Test
    public void testGetTodayFortune_AuthenticatedUser() {
        // Given
        Long userNum = 1L;

        // When
        ApiResponse<TodayFortuneDTO> response = fortuneController.getTodayFortune(userNum, null);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getResponseCode().getCode());
        assertNotNull(response.getData());
        assertEquals("테스트회원", response.getData().getUserNickname());
        assertEquals("데이터 엔지니어", response.getData().getLuckyJobName());
        assertEquals(90, response.getData().getOverallScore());
    }

    @Test
    public void testGetTodayFortune_GuestUser() {
        // Given
        Long userNum = null;

        // When
        ApiResponse<TodayFortuneDTO> response = fortuneController.getTodayFortune(userNum, null);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getResponseCode().getCode());
        assertNotNull(response.getData());
        assertEquals("취준생", response.getData().getUserNickname());
        assertEquals("병화(병인)", response.getData().getDayMaster());
    }
}
