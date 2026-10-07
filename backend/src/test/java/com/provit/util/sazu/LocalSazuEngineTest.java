package com.provit.util.sazu;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.Test;

/**
 * [LocalSazuEngineTest]
 * 60갑자 주기, 10천간 오행, 10십성 상생/상극 행렬, 12신살 삼합 규칙 등
 * 사주 명리 도메인 연산 규칙의 수학적 정합성을 검증하고 고정하는 단위 테스트
 */
public class LocalSazuEngineTest {

    @Test
    public void testAnchorDateIsGapJa() {
        // [기준일 Epoch 검증] 2024년 1월 1일은 갑자(甲子)일 (인덱스 0)
        LocalDate anchor = LocalDate.of(2024, 1, 1);
        int index = LocalSazuEngine.getGanjiIndex(anchor);
        assertEquals(0, index);
    }

    @Test
    public void testKnownBirthDateAndTodayInteraction() {
        // [천문 만세력 및 Sazu API 원본 대조 검증]
        // 1998년 5월 19일 (병인일: 인덱스 2)
        LocalDate birthDate = LocalDate.of(1998, 5, 19);
        int birthIndex = LocalSazuEngine.getGanjiIndex(birthDate);
        assertEquals(2, birthIndex); // 병인(2)

        // 2026년 7월 15일 (경인일)
        LocalDate targetDate = LocalDate.of(2026, 7, 15);

        LocalSazuEngine.SazuCalculationResult result = LocalSazuEngine.calculate(birthDate, targetDate);

        // 1. 일간 및 오행 검증
        assertEquals("병", result.getDayMasterSky());
        assertEquals("화", result.getDayMasterElement());
        assertTrue(result.getDayMaster().contains("병인"));

        // 2. 일진 및 천간 검증
        assertEquals("경", result.getTodayStem());
        assertTrue(result.getTodayIlju().startsWith("경인"));

        // 3. 십성 검증 (병화가 경금을 극함: 화극금, 양/양 동일음양 -> 편재)
        assertEquals("편재", result.getStemSipseong());

        // 4. 신살 검증 (일지 인, 오늘 인 -> 지살)
        assertEquals("지살", result.getSinsalName());
    }

    @Test
    public void testTenGodsCompleteMatrix() {
        // [10대 십성 행렬 완전성 검증]
        // 2024년 1월 1일은 갑자(甲子)일이므로 일간 = '갑'(양목)
        LocalDate userGap = LocalDate.of(2024, 1, 1);

        // 갑목(양목) 기준 10개 천간의 일진과 대조하여 모든 십성 규칙 고정
        // 2024-01-01: 갑자 (갑: 비견)
        assertEquals("비견", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 1)).getStemSipseong());
        // 2024-01-02: 을축 (을: 겁재)
        assertEquals("겁재", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 2)).getStemSipseong());
        // 2024-01-03: 병인 (병: 식신)
        assertEquals("식신", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 3)).getStemSipseong());
        // 2024-01-04: 정묘 (정: 상관)
        assertEquals("상관", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 4)).getStemSipseong());
        // 2024-01-05: 무진 (무: 편재)
        assertEquals("편재", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 5)).getStemSipseong());
        // 2024-01-06: 기사 (기: 정재)
        assertEquals("정재", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 6)).getStemSipseong());
        // 2024-01-07: 경오 (경: 편관)
        assertEquals("편관", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 7)).getStemSipseong());
        // 2024-01-08: 신미 (신: 정관)
        assertEquals("정관", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 8)).getStemSipseong());
        // 2024-01-09: 임신 (임: 편인)
        assertEquals("편인", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 9)).getStemSipseong());
        // 2024-01-10: 계유 (계: 정인)
        assertEquals("정인", LocalSazuEngine.calculate(userGap, LocalDate.of(2024, 1, 10)).getStemSipseong());
    }

    @Test
    public void testTwelveSinsalSamhapGroups() {
        // [12신살 4대 삼합국 규칙 고정 검증]
        // 1. 인오술 화국 (생지: 인)
        // 1998-05-19: 병인일 (일지: 인 -> 화국)
        LocalDate userIn = LocalDate.of(1998, 5, 19);
        // 오늘 일지가 인(2)일 때 -> 지살 (생지 시작)
        assertEquals("지살", LocalSazuEngine.calculate(userIn, LocalDate.of(2026, 7, 15)).getSinsalName());
        // 오늘 일지가 오(6)일 때 -> 장성살 (화국 왕지)
        assertEquals("장성살", LocalSazuEngine.calculate(userIn, LocalDate.of(2026, 7, 19)).getSinsalName());

        // 2. 신자진 수국 (생지: 신)
        // 2024-01-01: 갑자일 (일지: 자 -> 수국)
        LocalDate userJa = LocalDate.of(2024, 1, 1);
        // 오늘 일지가 신(8)일 때 -> 지살 (수국 생지)
        assertEquals("지살", LocalSazuEngine.calculate(userJa, LocalDate.of(2024, 1, 9)).getSinsalName());
        // 오늘 일지가 자(0)일 때 -> 장성살 (수국 왕지)
        assertEquals("장성살", LocalSazuEngine.calculate(userJa, LocalDate.of(2024, 1, 1)).getSinsalName());
    }

    @Test
    public void testLeapYearContinuity() {
        // [윤년 2월 29일 연속성 검증]
        // 2024년 2월 28일 -> 2024년 2월 29일 -> 2024년 3월 1일이 간지 인덱스 1씩 연속 증가하는지 검증
        int feb28 = LocalSazuEngine.getGanjiIndex(LocalDate.of(2024, 2, 28));
        int feb29 = LocalSazuEngine.getGanjiIndex(LocalDate.of(2024, 2, 29));
        int mar01 = LocalSazuEngine.getGanjiIndex(LocalDate.of(2024, 3, 1));

        assertEquals((feb28 + 1) % 60, feb29);
        assertEquals((feb29 + 1) % 60, mar01);
    }

    @Test
    public void testNullSafety() {
        // [Null Safety 검증] null 전달 시 기본 생년월일(1998-05-19) 및 오늘 날짜로 방어 동작
        LocalSazuEngine.SazuCalculationResult result = LocalSazuEngine.calculate(null, null);

        assertNotNull(result);
        assertNotNull(result.getDayMaster());
        assertNotNull(result.getDayMasterElement());
        assertNotNull(result.getStemSipseong());
        assertNotNull(result.getTodayIlju());
        assertNotNull(result.getSinsalName());
    }

    @Test
    public void testArbitraryMemberBirthDatesProduceDistinctPersonalization() {
        // [회원별 고유성 검증] 서로 다른 회원은 서로 다른 일간과 십성을 부여받아야 함
        LocalDate today = LocalDate.of(2026, 10, 6);

        // 회원 A: 1995년 3월 15일
        LocalSazuEngine.SazuCalculationResult resultA = LocalSazuEngine.calculate(LocalDate.of(1995, 3, 15), today);

        // 회원 B: 2000년 11월 20일
        LocalSazuEngine.SazuCalculationResult resultB = LocalSazuEngine.calculate(LocalDate.of(2000, 11, 20), today);

        assertNotNull(resultA.getStemSipseong());
        assertNotNull(resultB.getStemSipseong());
        assertFalse(resultA.getDayMaster().equals(resultB.getDayMaster()));
    }
}
