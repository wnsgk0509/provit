package com.provit.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 팀원 공통 유틸리티 클래스 (날짜 변환, 문자열 처리 등)
 */
public class CommonUtil {

    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private CommonUtil() {
        // 인스턴스화 방지 유틸리티 클래스
    }

    /**
     * 현재 일시를 yyyy-MM-dd HH:mm:ss 형식의 문자열로 반환
     */
    public static String getCurrentDateTimeStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT));
    }

    /**
     * 문자열이 null이거나 비어있는지 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
