package com.provit.dto.fortune;

import java.util.List;

import com.provit.dto.recruitment.RecruitmentDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오늘의 취업 운세 및 행운 추천 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayFortuneDTO {

    /** 운세 기준 일자 (예: 2026-10-02) */
    private String fortuneDate;

    /** 사용자 닉네임 */
    private String userNickname;

    /** 나의 사주 일간 (예: 갑목(甲木) - 곧고 당당한 거목의 기운) */
    private String dayMaster;

    /** 나의 일간 오행 (목, 화, 토, 금, 수) */
    private String dayMasterElement;

    /** 오늘의 일진 간지 (예: 무인(戊寅)일) */
    private String todayIlju;

    /** 오늘 일진의 오행 (예: 토/목) */
    private String todayElement;

    /** 내 일간과 오늘의 십성 관계 (예: 편재(偏財)) */
    private String tenGodsRelation;

    /** 십성의 취업 의미 설명 */
    private String tenGodsMeaning;

    /** 오늘의 취업 운세 총점 (100점 만점) */
    private int overallScore;

    /** 오늘의 종합 취업 총운 총평 (2~3문장) */
    private String overallSummary;

    /** 오늘의 한 줄 행동 조언 */
    private String advice;

    /** 오늘의 행운 직무 (채용 공고 결합 매칭 키워드) */
    private String luckyJobName;

    /** 오늘의 행운 키워드 */
    private String luckyKeyword;

    /** 행운의 색상 */
    private String luckyColor;

    /** 행운의 방향 */
    private String luckyDirection;

    /** 행운의 숫자 */
    private int luckyNumber;

    /** 오늘의 12신살 명칭 (예: 장성살, 역마살, 화개살 등) */
    private String sinsalName;

    /** 신살 맞춤 취업 조언 */
    private String sinsalAdvice;

    /** 운세 기반 오늘의 추천 채용 공고 목록 (최대 6건) */
    private List<RecruitmentDTO> recommendRecruitments;
}
