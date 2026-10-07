package com.provit.util.sazu;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * [LocalSazuEngine] 로컬 만세력 및 명리 연산 엔진
 * 
 * 외부 Sazu API의 샌드박스 제한(Free 티어 샘플 5종 외 400 반환) 및 네트워크 장애 상황에서도
 * 사용자의 실제 양력 생년월일을 바탕으로 60갑자(천간·지지), 일간 오행, 십성(十星), 12신살(神殺)을
 * 100% 자체 연산하여 1:1 맞춤 운세를 도출합니다.
 */
public final class LocalSazuEngine {

    private LocalSazuEngine() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // [기준점 Epoch]: 2024년 1월 1일은 갑자(甲子, index 0)일
    private static final LocalDate ANCHOR_DATE = LocalDate.of(2024, 1, 1);

    // 10 천간 (0~9)
    private static final String[] STEMS = {
        "갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"
    };

    // 10 천간 상징 명칭
    private static final String[] STEM_TITLES = {
        "갑목(甲木) - 곧게 뻗어나가는 거목",
        "을목(乙木) - 유연하고 끈기 있는 넝쿨",
        "병화(丙火) - 세상을 비추는 태양의 열정",
        "정화(丁火) - 어둠을 밝히는 따뜻한 촛불",
        "무토(戊土) - 묵직하고 든든한 태산",
        "기토(己土) - 만물을 길러내는 비옥한 전답",
        "경금(庚金) - 단단하고 결단력 있는 원석",
        "신금(辛金) - 섬세하고 예리한 보석",
        "임수(壬水) - 넓고 깊은 바다의 지혜",
        "계수(癸水) - 만물을 적시는 촉촉한 봄비"
    };

    // 천간 오행 ("목", "목", "화", "화", "토", "토", "금", "금", "수", "수")
    private static final String[] STEM_ELEMENTS = {
        "목", "목", "화", "화", "토", "토", "금", "금", "수", "수"
    };

    // 12 지지 (0~11)
    private static final String[] BRANCHES = {
        "자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"
    };

    // 지지 오행 ("수", "토", "목", "목", "토", "화", "화", "토", "금", "금", "토", "수")
    private static final String[] BRANCH_ELEMENTS = {
        "수", "토", "목", "목", "토", "화", "화", "토", "금", "금", "토", "수"
    };

    // 60갑자 명칭 캐시
    private static final String[] GANJI_60 = new String[60];
    static {
        for (int i = 0; i < 60; i++) {
            GANJI_60[i] = STEMS[i % 10] + BRANCHES[i % 12];
        }
    }

    /**
     * 생년월일과 대상 일자(오늘)를 기반으로 사주 명리 상호작용 계산
     *
     * @param birthDate 사용자 양력 생년월일 (null일 경우 1998-05-19 기본값 적용)
     * @param targetDate 오늘 날짜 (null일 경우 LocalDate.now())
     * @return SazuCalculationResult 계산 결과 객체
     */
    public static SazuCalculationResult calculate(LocalDate birthDate, LocalDate targetDate) {
        LocalDate birth = (birthDate != null) ? birthDate : LocalDate.of(1998, 5, 19);
        LocalDate today = (targetDate != null) ? targetDate : LocalDate.now();

        // 1. 사용자 일주(Day Pillar) 계산
        int birthGanjiIdx = getGanjiIndex(birth);
        int userStemIdx = birthGanjiIdx % 10;
        int userBranchIdx = birthGanjiIdx % 12;

        String dayMasterFull = STEM_TITLES[userStemIdx] + " (" + GANJI_60[birthGanjiIdx] + ")";
        String dayMasterElement = STEM_ELEMENTS[userStemIdx];

        // 2. 오늘의 일진(Today Pillar) 계산
        int todayGanjiIdx = getGanjiIndex(today);
        int todayStemIdx = todayGanjiIdx % 10;
        int todayBranchIdx = todayGanjiIdx % 12;

        String todayIlju = GANJI_60[todayGanjiIdx] + "일";
        String todayStem = STEMS[todayStemIdx];
        String todayStemElement = STEM_ELEMENTS[todayStemIdx];
        String todayBranchElement = BRANCH_ELEMENTS[todayBranchIdx];
        String todayElement = todayStemElement + " / " + todayBranchElement;

        // 3. 일간과 오늘 천간 사이의 십성(十星) 계산
        String stemSipseong = calculateSipseong(userStemIdx, todayStemIdx);

        // 4. 일지와 오늘 지지 사이의 12신살(神殺) 계산
        String sinsalName = calculateSinsal(userBranchIdx, todayBranchIdx);

        return new SazuCalculationResult(
            dayMasterFull,
            STEMS[userStemIdx],
            dayMasterElement,
            todayIlju,
            todayStem,
            todayStemElement,
            todayElement,
            stemSipseong,
            sinsalName
        );
    }

    /**
     * 날짜의 60갑자 인덱스(0~59) 계산 (2024-01-01 갑자 기준)
     */
    public static int getGanjiIndex(LocalDate date) {
        long diffDays = ChronoUnit.DAYS.between(ANCHOR_DATE, date);
        return (int) ((diffDays % 60 + 60) % 60);
    }

    /**
     * 일간(userStem)과 오늘 천간(todayStem)의 오행 및 음양 관계를 통한 십성(十星) 계산
     * 
     * - 비견/겁재 (동일 오행)
     * - 식신/상관 (내가 생함: 목->화->토->금->수)
     * - 편재/정재 (내가 극함: 목->토, 화->금, 토->수, 금->목, 수->화)
     * - 편관/정관 (나를 극함)
     * - 편인/정인 (나를 생함)
     */
    private static String calculateSipseong(int userStemIdx, int todayStemIdx) {
        int userElem = userStemIdx / 2;   // 0:목, 1:화, 2:토, 3:금, 4:수
        int todayElem = todayStemIdx / 2; // 0:목, 1:화, 2:토, 3:금, 4:수

        boolean samePolarity = (userStemIdx % 2) == (todayStemIdx % 2); // 음양 일치 여부

        // 오행 순환 거리 (0:동일, 1:아생, 2:아극, 3:극아, 4:생아)
        int diff = (todayElem - userElem + 5) % 5;

        switch (diff) {
            case 0:
                return samePolarity ? "비견" : "겁재";
            case 1:
                return samePolarity ? "식신" : "상관";
            case 2:
                return samePolarity ? "편재" : "정재";
            case 3:
                return samePolarity ? "편관" : "정관";
            case 4:
            default:
                return samePolarity ? "편인" : "정인";
        }
    }

    /**
     * 일지(userBranch)와 오늘 지지(todayBranch)의 삼합(三合) 기준 12신살(神殺) 계산
     */
    private static String calculateSinsal(int userBranchIdx, int todayBranchIdx) {
        // 12지지: 0:자, 1:축, 2:인, 3:묘, 4:진, 5:사, 6:오, 7:미, 8:신, 9:유, 10:술, 11:해
        int groupBase;

        // 삼합국 기준 시작 지지(생지) 결정
        if (userBranchIdx == 2 || userBranchIdx == 6 || userBranchIdx == 10) {
            // 인오술(寅午戌) 화국 -> 시작: 인(2)
            groupBase = 2;
        } else if (userBranchIdx == 8 || userBranchIdx == 0 || userBranchIdx == 4) {
            // 신자진(申子辰) 수국 -> 시작: 신(8)
            groupBase = 8;
        } else if (userBranchIdx == 5 || userBranchIdx == 9 || userBranchIdx == 1) {
            // 사유축(巳酉丑) 금국 -> 시작: 사(5)
            groupBase = 5;
        } else {
            // 해묘미(亥卯未) 목국 -> 시작: 해(11)
            groupBase = 11;
        }

        // groupBase로부터 순차적 12신살 매핑
        int step = (todayBranchIdx - groupBase + 12) % 12;

        switch (step) {
            case 0: return "지살";
            case 1: return "년살(도화살)";
            case 2: return "월살";
            case 3: return "망신살";
            case 4: return "장성살";
            case 5: return "반안살";
            case 6: return "역마살";
            case 7: return "육해살";
            case 8: return "화개살";
            case 9: return "겁살";
            case 10: return "재살";
            case 11:
            default: return "천살";
        }
    }

    /**
     * 계산 결과 VO
     */
    public static class SazuCalculationResult {
        private final String dayMaster;
        private final String dayMasterSky;
        private final String dayMasterElement;
        private final String todayIlju;
        private final String todayStem;
        private final String todayStemElement;
        private final String todayElement;
        private final String stemSipseong;
        private final String sinsalName;

        public SazuCalculationResult(String dayMaster, String dayMasterSky, String dayMasterElement,
                                     String todayIlju, String todayStem, String todayStemElement,
                                     String todayElement, String stemSipseong, String sinsalName) {
            this.dayMaster = dayMaster;
            this.dayMasterSky = dayMasterSky;
            this.dayMasterElement = dayMasterElement;
            this.todayIlju = todayIlju;
            this.todayStem = todayStem;
            this.todayStemElement = todayStemElement;
            this.todayElement = todayElement;
            this.stemSipseong = stemSipseong;
            this.sinsalName = sinsalName;
        }

        public String getDayMaster() { return dayMaster; }
        public String getDayMasterSky() { return dayMasterSky; }
        public String getDayMasterElement() { return dayMasterElement; }
        public String getTodayIlju() { return todayIlju; }
        public String getTodayStem() { return todayStem; }
        public String getTodayStemElement() { return todayStemElement; }
        public String getTodayElement() { return todayElement; }
        public String getStemSipseong() { return stemSipseong; }
        public String getSinsalName() { return sinsalName; }
    }
}
