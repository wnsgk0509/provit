package com.provit.util.sazu;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.Test;

public class LocalSazuEngineTest {

    @Test
    public void testAnchorDateIsGapJa() {
        // 2024년 1월 1일은 갑자(甲子)일 (인덱스 0)
        LocalDate anchor = LocalDate.of(2024, 1, 1);
        int index = LocalSazuEngine.getGanjiIndex(anchor);
        assertEquals(0, index);
    }

    @Test
    public void testKnownBirthDateAndTodayInteraction() {
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
    public void testNullSafety() {
        // null 전달 시 기본 생년월일(1998-05-19) 및 오늘 날짜로 방어 동작
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
        LocalDate today = LocalDate.of(2026, 10, 6);

        // 회원 A: 1995년 3월 15일
        LocalSazuEngine.SazuCalculationResult resultA = LocalSazuEngine.calculate(LocalDate.of(1995, 3, 15), today);

        // 회원 B: 2000년 11월 20일
        LocalSazuEngine.SazuCalculationResult resultB = LocalSazuEngine.calculate(LocalDate.of(2000, 11, 20), today);

        System.out.println("회원 A 일간: " + resultA.getDayMaster() + " | 십성: " + resultA.getStemSipseong());
        System.out.println("회원 B 일간: " + resultB.getDayMaster() + " | 십성: " + resultB.getStemSipseong());

        // 두 회원의 일간이나 십성이 회원별로 고유하게 도출되는지 확인
        assertNotNull(resultA.getStemSipseong());
        assertNotNull(resultB.getStemSipseong());
        assertNotEquals(resultA.getDayMaster(), resultB.getDayMaster());
    }
}
