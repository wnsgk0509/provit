package com.provit.common;

import lombok.Getter;

/**
 * GNB(상단 네비게이션 헤더) 메뉴 목록 Enum
 */
@Getter
public enum NavMenu {
    HOME("/", "홈"),
    JOBS("/jobs", "채용 공고"),
    FORTUNE("/fortune", "오늘의 운세"),
    INTERVIEW("/interview", "AI 모의 면접실"),
    STUDY("/study", "스터디 모집"),
    MYPAGE("/mypage", "마이페이지");

    private final String path;
    private final String title;

    NavMenu(String path, String title) {
        this.path = path;
        this.title = title;
    }
}
