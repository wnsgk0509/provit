package com.provit.service.fortune.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.provit.client.fortune.SazuApiClient;
import com.provit.client.fortune.dto.SazuTodayRequestDTO;
import com.provit.dao.auth.UserDAO;
import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.fortune.TodayFortuneDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.service.fortune.FortuneService;
import com.provit.util.sazu.LocalSazuEngine;

/**
 * 오늘의 취업 운세 및 행운 추천 공고 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
public class FortuneServiceImpl implements FortuneService {

    private static final Logger log = LoggerFactory.getLogger(FortuneServiceImpl.class);

    private final SazuApiClient sazuApiClient;
    private final UserDAO userDAO;
    private final RecruitmentDAO recruitmentDAO;

    @Autowired
    public FortuneServiceImpl(SazuApiClient sazuApiClient, UserDAO userDAO, RecruitmentDAO recruitmentDAO) {
        this.sazuApiClient = sazuApiClient;
        this.userDAO = userDAO;
        this.recruitmentDAO = recruitmentDAO;
    }

    @Override
    public TodayFortuneDTO getTodayFortune(Long userNum) {
        log.info(">> [FortuneService] 오늘의 취업 운세 조회 시작: userNum={}", userNum);

        String userNickname = "취준생";
        SazuTodayRequestDTO requestDTO = SazuTodayRequestDTO.defaultSample();
        LocalDate userBirthDate = null;

        // 1. 사용자 정보 확인 및 생년월일 파싱
        if (userNum != null) {
            try {
                UserDTO userDTO = userDAO.selectByUserNum(userNum);
                if (userDTO != null) {
                    if (userDTO.getUserNickname() != null && !userDTO.getUserNickname().isBlank()) {
                        userNickname = userDTO.getUserNickname();
                    }

                    if (userDTO.getUserBirthDate() != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(userDTO.getUserBirthDate());
                        int birthYear = cal.get(Calendar.YEAR);
                        int birthMonth = cal.get(Calendar.MONTH) + 1;
                        int birthDay = cal.get(Calendar.DAY_OF_MONTH);
                        userBirthDate = LocalDate.of(birthYear, birthMonth, birthDay);

                        requestDTO = SazuTodayRequestDTO.builder()
                                .birthYear(birthYear)
                                .birthMonth(birthMonth)
                                .birthDay(birthDay)
                                .birthHour(9)
                                .birthMinute(0)
                                .isLunar(false)
                                .isFemale(false)
                                .birthCity("서울")
                                .build();
                        log.info(">> [FortuneService] 회원 생년월일 적용: {}", userBirthDate);
                    }
                }
            } catch (Exception e) {
                log.warn(">> [FortuneService] 회원 정보 조회 중 예외 발생, 기본 프로필 적용: {}", e.getMessage());
            }
        }

        // 2. 사주 API 키 티어 판별 및 운세 엔진 분기
        // - 유료(Pro/Enterprise) 키: 외부 sazu.app REST API를 직접 호출하여 데이터 추출
        // - 무료(Free 샌드박스) 키: sazu_free_... 키는 고정된 특정 샘플 1인의 데이터만 반환하므로,
        //   모든 회원의 실제 생년월일 기반 1:1 개인화 운세를 위해 LocalSazuEngine을 직접 구동
        // - 미설정/통신 장애: LocalSazuEngine으로 안전하게 전환
        if (sazuApiClient.isConfigured() && !sazuApiClient.isFreeTier()) {
            try {
                JsonNode data = sazuApiClient.getTodayFortune(requestDTO);
                return parseAndBuildFortuneFromApi(data, userNickname, userNum);
            } catch (Exception e) {
                log.warn(">> [FortuneService] 유료 사주 API 호출 실패. LocalSazuEngine 기반 정밀 계산으로 전환합니다: {}", e.getMessage());
                return buildLocalEngineFortune(userNickname, userBirthDate, userNum);
            }
        } else {
            log.info(">> [FortuneService] 무료(Free 샌드박스) API 환경 감지: 전 회원 실제 생년월일 기반 1:1 맞춤 운세를 위해 LocalSazuEngine을 직접 가동합니다.");
            return buildLocalEngineFortune(userNickname, userBirthDate, userNum);
        }
    }

    /**
     * Sazu API 성공 응답 데이터를 TodayFortuneDTO로 변환
     */
    private TodayFortuneDTO parseAndBuildFortuneFromApi(JsonNode data, String userNickname, Long userNum) {
        String fortuneDate = data.path("reference").path("date").asText(LocalDate.now().toString());

        JsonNode fourPillars = data.path("modules").path("fourPillars");
        String dayMasterSky = fourPillars.path("day").path("skyFull").asText("병화");
        String dayMasterElement = fourPillars.path("day").path("skyElement").asText("화");
        String dayMasterFull = dayMasterSky + "(" + fourPillars.path("day").path("full").asText("병인") + ")";

        JsonNode dailyInteraction = data.path("modules").path("dailyInteraction");
        String todayIlju = dailyInteraction.path("ilju").path("ganji").asText("무인") + "일";
        String stemElem = dailyInteraction.path("ilju").path("stemElement").asText("토");
        String branchElem = dailyInteraction.path("ilju").path("branchElement").asText("목");
        String todayElement = stemElem + " / " + branchElem;

        String stemSipseong = dailyInteraction.path("toDayMaster").path("stemSipseong").asText("정관");

        // 신살(神殺) 명칭 및 기본 의미 추출
        String sinsalName = "장성살";
        String sinsalOriginalMeaning = "";
        JsonNode sinsalArray = dailyInteraction.path("sinsal");
        if (sinsalArray.isArray() && sinsalArray.size() > 0) {
            JsonNode firstSinsal = sinsalArray.get(0);
            sinsalName = firstSinsal.path("name").asText("장성살");
            sinsalOriginalMeaning = firstSinsal.path("meaning").asText("");
        } else {
            JsonNode angels = data.path("modules").path("sinsal").path("angels");
            if (angels.isArray() && angels.size() > 0) {
                sinsalName = angels.get(0).path("name").asText("귀인");
                sinsalOriginalMeaning = angels.get(0).path("description").asText("");
            }
        }

        // 4. 명리학 기반 취업 운세 해석 및 아이템 도출
        TenGodsInterpretation interpretation = interpretTenGods(stemSipseong);
        ElementFortune itemFortune = interpretElement(stemElem);
        String sinsalAdvice = interpretSinsal(sinsalName, sinsalOriginalMeaning);

        // 5. 행운 직무 키워드 기반 채용 공고 결합 (최대 6건, 부족 시 HOT 공고로 채움)
        List<RecruitmentDTO> recommendList = fetchRecommendRecruitments(interpretation.luckyJobKeyword, userNum, 6);

        // 6. 최종 TodayFortuneDTO 완성
        return TodayFortuneDTO.builder()
                .fortuneDate(fortuneDate)
                .userNickname(userNickname)
                .dayMaster(dayMasterFull)
                .dayMasterElement(dayMasterElement)
                .todayIlju(todayIlju)
                .todayElement(todayElement)
                .tenGodsRelation(stemSipseong)
                .tenGodsMeaning(interpretation.meaning)
                .overallScore(interpretation.score)
                .overallSummary(interpretation.summary)
                .advice(interpretation.advice)
                .luckyJobName(interpretation.luckyJobDisplay)
                .luckyKeyword(interpretation.luckyKeyword)
                .luckyColor(itemFortune.color)
                .luckyDirection(itemFortune.direction)
                .luckyNumber(itemFortune.number)
                .sinsalName(sinsalName)
                .sinsalAdvice(sinsalAdvice)
                .recommendRecruitments(recommendList)
                .build();
    }

    /**
     * 행운 직무 키워드로 공고를 검색하고, 부족할 경우 HOT 인기 공고로 채워 6건을 보장
     */
    private List<RecruitmentDTO> fetchRecommendRecruitments(String keyword, Long userNum, int targetCount) {
        List<RecruitmentDTO> results = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // 1차: 행운 직무 키워드 공고 검색
        try {
            List<RecruitmentDTO> keywordList = recruitmentDAO.selectRecruitmentsByKeyword(keyword, userNum, targetCount);
            if (keywordList != null) {
                for (RecruitmentDTO dto : keywordList) {
                    if (dto.getRecruitmentNum() != null && seenIds.add(dto.getRecruitmentNum())) {
                        results.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.warn(">> [FortuneService] 키워드 공고 조회 예외: {}", e.getMessage());
        }

        // 2차: 6건 미만인 경우 실시간 인기/최신 공고로 보충
        if (results.size() < targetCount) {
            try {
                int needed = targetCount - results.size();
                List<RecruitmentDTO> hotList = recruitmentDAO.selectHotRecruitments(userNum, targetCount * 2);
                if (hotList != null) {
                    for (RecruitmentDTO dto : hotList) {
                        if (dto.getRecruitmentNum() != null && seenIds.add(dto.getRecruitmentNum())) {
                            results.add(dto);
                            if (results.size() >= targetCount) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn(">> [FortuneService] HOT 공고 보충 조회 예외: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * 십성(十星) 기반 취업 운세 해석
     */
    private TenGodsInterpretation interpretTenGods(String sipseong) {
        if (sipseong == null) sipseong = "정관";

        switch (sipseong) {
            case "비견":
                return new TenGodsInterpretation(
                        "나와 대등한 동료와 어깨를 나란히 하며 자립심과 건강한 경쟁력을 발휘하는 기운입니다.",
                        88,
                        "오늘은 팀 프로젝트 경험과 협업 성과를 이력서에 강조하기에 매우 좋은 날입니다. 동료나 스터디원들과 피드백을 주고받으면 이력서의 완성도가 한층 높아집니다.",
                        "동료 또는 스터디원과 모의면접 피드백을 나누며 객관적인 조언을 적극 수용해보세요.",
                        "백엔드 개발자",
                        "백엔드",
                        "팀워크와 코드 리뷰"
                );
            case "겁재":
                return new TenGodsInterpretation(
                        "치열한 경쟁 속에서도 과감한 승부욕과 빠른 실행력으로 난관을 돌파하는 기운입니다.",
                        84,
                        "경쟁률이 높은 기업 공고에서도 나만의 뚜렷한 강점을 무기로 돋보일 수 있는 날입니다. 난관을 극복한 트러블슈팅 경험을 설득력 있게 제시하면 면접관의 시선을 사로잡습니다.",
                        "남들과 차별화되는 나만의 핵심 문제 해결 경험 1가지를 포트폴리오에 명확히 부각하세요.",
                        "보안 / 인프라 엔지니어",
                        "인프라",
                        "트러블슈팅 돌파력"
                );
            case "식신":
                return new TenGodsInterpretation(
                        "본인의 타고난 전문 기술과 창의적인 역량을 세상에 멋지게 구현해내는 풍요로운 기운입니다.",
                        95,
                        "갈고닦은 코딩 실력과 기술적 아이디어가 술술 풀리는 최고의 날입니다. 포트폴리오의 아키텍처 다이어그램을 다듬거나 깃허브 리포지토리를 정비하여 자신 있게 지원서를 제출하세요.",
                        "포트폴리오의 UI/UX 완성도와 기술 스택 도입 이유를 상세하게 보강해보세요.",
                        "프론트엔드 개발자",
                        "프론트",
                        "창의적 UI/UX와 구현력"
                );
            case "상관":
                return new TenGodsInterpretation(
                        "번뜩이는 재치와 뛰어난 언변, 기존의 틀을 깨는 혁신적인 기획력을 발휘하는 기운입니다.",
                        92,
                        "면접관의 까다로운 기술 면접이나 돌발 질문에도 센스 있는 답변으로 주도권을 잡을 수 있는 날입니다. 새로운 접근 방식과 개선 아이디어가 담긴 포트폴리오가 큰 호평을 받습니다.",
                        "모의면접 연습 시 결론을 먼저 말하는 두괄식 화법으로 논리성을 각인시키세요.",
                        "웹 풀스택 개발자",
                        "웹",
                        "설득력 있는 기술 면접"
                );
            case "편재":
                return new TenGodsInterpretation(
                        "업계 트렌드와 채용 시장의 변화를 예리하게 포착하고 큰 기회를 선점하는 기운입니다.",
                        90,
                        "성장 잠재력이 높고 본인의 가치를 알아봐 줄 유망 기업을 발굴하기에 최적의 날입니다. 수치화된 성과 지표(성능 개선율, 비용 절감 등)를 이력서에 반영하면 서류 합격률이 급상승합니다.",
                        "프로젝트 성과를 정량적인 숫자와 명확한 지표로 이력서에 반영해보세요.",
                        "데이터 엔지니어",
                        "데이터",
                        "정량적 성과 분석"
                );
            case "정재":
                return new TenGodsInterpretation(
                        "빈틈없는 성실함과 꼼꼼한 마감 능력으로 신뢰를 탄탄하게 쌓아 올리는 기운입니다.",
                        93,
                        "기본기가 단단하고 성실한 인재로서 면접관에게 신뢰를 줄 수 있는 날입니다. 이력서 오탈자, 포트폴리오 링크 작동 여부, 마감 기한 등을 최종 점검하여 침착하게 원서를 접수하세요.",
                        "포트폴리오의 배포 데모 링크와 깃허브 리포지토리가 정상 작동하는지 꼼꼼히 점검하세요.",
                        "시스템 / 백엔드 엔지니어",
                        "개발",
                        "신뢰도 높은 기본기"
                );
            case "편관":
                return new TenGodsInterpretation(
                        "높은 난도의 과제를 묵묵히 완수하고 강한 책임감과 위기 극복 역량을 증명하는 기운입니다.",
                        86,
                        "까다로운 코딩 테스트나 기술 과제 전형에서 강한 집중력을 발휘해 놀라운 성과를 낼 수 있는 날입니다. 복잡한 문제를 끝까지 파고들어 해결해낸 집념을 서류와 면접에서 보여주세요.",
                        "과제 전형이나 코딩 테스트 시 엣지 케이스와 예외 처리를 철저하게 확인하세요.",
                        "클라우드 아키텍트",
                        "클라우드",
                        "과제 전형 극복과 끈기"
                );
            case "정관":
                return new TenGodsInterpretation(
                        "합격의 문이 활짝 열리고 공신력 있는 조직 및 우수 기업과의 인연이 강하게 닿는 기운입니다.",
                        98,
                        "모든 취업 기운 중 가장 귀한 합격운과 조직운이 최고조에 달하는 날입니다. 평소 가고 싶었던 기업의 채용 공고에 주저 없이 원서를 접수하고 면접 제안을 당당하게 수락하세요.",
                        "망설였던 주요 기업이나 공채 전형에 주저 없이 적극적으로 지원서를 제출하세요.",
                        "소프트웨어 엔지니어",
                        "소프트웨어",
                        "당당한 최종 합격운"
                );
            case "편인":
                return new TenGodsInterpretation(
                        "남들이 미처 보지 못한 틈새 기술과 깊이 있는 기술 아키텍처를 전략적으로 탐구하는 기운입니다.",
                        91,
                        "독보적인 기술 역량과 나만의 깊은 개발 철학이 빛을 발하는 날입니다. AI/머신러닝이나 내부 동작 원리 분석 등 남들과 차별화되는 전문 인사이트를 포트폴리오에 녹여내세요.",
                        "단순 기능 구현을 넘어 라이브러리 내부 동작 원리와 한계점을 깊이 있게 정리하세요.",
                        "인공지능 / AI 엔지니어",
                        "AI",
                        "깊이 있는 기술 철학"
                );
            case "정인":
            default:
                return new TenGodsInterpretation(
                        "스승이나 선배 멘토의 도움을 받고, 배움과 학업적 성취를 높게 인정받는 귀인의 기운입니다.",
                        97,
                        "서류 전형 통과율이 매우 높고 면접관에게 긍정적인 인상을 심어주기에 최적인 날입니다. 자기소개서에서 배움에 대한 진정성 있는 태도와 지속적인 성장 의지를 강조하세요.",
                        "현직자 멘토링이나 지인의 추천 채용 기회를 적극적으로 모색해보세요.",
                        "웹 / 앱 풀스택 개발자",
                        "개발",
                        "성장 잠재력과 귀인의 조력"
                );
        }
    }

    /**
     * 오행(五行) 기반 행운의 아이템 매핑
     */
    private ElementFortune interpretElement(String element) {
        if (element == null) element = "금";

        switch (element) {
            case "목":
                return new ElementFortune("싱그러운 포레스트 그린", "동쪽 (창가나 채광이 좋은 자리)", 3);
            case "화":
                return new ElementFortune("열정적인 코랄 레드 & 오렌지", "남쪽 (햇살이 잘 드는 공간)", 7);
            case "토":
                return new ElementFortune("포근한 웜 베이지 & 옐로우", "중앙 (안정적이고 편안한 실내)", 5);
            case "수":
                return new ElementFortune("신뢰감을 주는 딥 네이비", "북쪽 (차분하고 집중하기 좋은 서재)", 1);
            case "금":
            default:
                return new ElementFortune("깔끔한 퓨어 화이트 & 실버", "서쪽 (정돈되고 조용한 공간)", 9);
        }
    }

    /**
     * 신살(神殺) 기반 취업 맞춤 조언 매핑
     */
    private String interpretSinsal(String sinsalName, String originalMeaning) {
        if (sinsalName == null) sinsalName = "";

        if (sinsalName.contains("장성") || sinsalName.contains("건록")) {
            return "만인을 이끄는 리더십과 카리스마가 돋보입니다. 면접에서 주도적인 태도로 답변을 이끌어보세요.";
        } else if (sinsalName.contains("반안")) {
            return "합격의 안장에 오르는 길운입니다. 공들여 쓴 지원서에서 기분 좋은 서류 합격 소식을 기대해도 좋습니다.";
        } else if (sinsalName.contains("문곡") || sinsalName.contains("화개")) {
            return "글재주와 기획력이 최고조에 달합니다. 자기소개서 첨삭과 기술 블로그 글을 다듬기에 가장 좋은 날입니다.";
        } else if (sinsalName.contains("학당") || sinsalName.contains("천주")) {
            return "학습 흡수력이 뛰어나고 지적 성취가 높은 날입니다. 코딩 테스트 대비나 CS 기초 이론을 집중 정리하세요.";
        } else if (sinsalName.contains("지살") || sinsalName.contains("역마")) {
            return "새로운 환경으로의 확장이 유리한 날입니다. 원격 근무나 판교/강남 등 주요 IT 허브 기업의 공고를 탐색해보세요.";
        } else if (sinsalName.contains("홍염")) {
            return "자연스러운 호감과 매력이 돋보이는 날입니다. 면접관과의 첫인사에서 밝고 긍정적인 미소로 신뢰를 얻으세요.";
        } else if (sinsalName.contains("망신") || sinsalName.contains("탕화")) {
            return "의욕이 앞서 실수를 부를 수 있습니다. 면접 질문의 의도를 끝까지 경청한 뒤 핵심부터 침착하게 답변하세요.";
        }

        if (originalMeaning != null && !originalMeaning.isBlank()) {
            return originalMeaning;
        }

        return "오늘 하루 당신의 꾸준한 노력이 취업 성공을 향한 가장 든든한 디딤돌이 되어줄 것입니다.";
    }

    /**
     * LocalSazuEngine을 통해 회원 실제 생년월일 기반 1:1 맞춤 운세 모델 동적 생성
     */
    private TodayFortuneDTO buildLocalEngineFortune(String userNickname, LocalDate userBirthDate, Long userNum) {
        log.info(">> [FortuneService] LocalSazuEngine 정밀 계산 시작: userNickname={}, userBirthDate={}", userNickname, userBirthDate);

        LocalSazuEngine.SazuCalculationResult result = LocalSazuEngine.calculate(userBirthDate, LocalDate.now());

        TenGodsInterpretation interpretation = interpretTenGods(result.getStemSipseong());
        ElementFortune itemFortune = interpretElement(result.getTodayStemElement());
        String sinsalAdvice = interpretSinsal(result.getSinsalName(), "");

        List<RecruitmentDTO> recommendList = fetchRecommendRecruitments(interpretation.luckyJobKeyword, userNum, 6);

        return TodayFortuneDTO.builder()
                .fortuneDate(LocalDate.now().toString())
                .userNickname(userNickname)
                .dayMaster(result.getDayMaster())
                .dayMasterElement(result.getDayMasterElement())
                .todayIlju(result.getTodayIlju())
                .todayElement(result.getTodayElement())
                .tenGodsRelation(result.getStemSipseong())
                .tenGodsMeaning(interpretation.meaning)
                .overallScore(interpretation.score)
                .overallSummary(interpretation.summary)
                .advice(interpretation.advice)
                .luckyJobName(interpretation.luckyJobDisplay)
                .luckyKeyword(interpretation.luckyKeyword)
                .luckyColor(itemFortune.color)
                .luckyDirection(itemFortune.direction)
                .luckyNumber(itemFortune.number)
                .sinsalName(result.getSinsalName())
                .sinsalAdvice(sinsalAdvice)
                .recommendRecruitments(recommendList)
                .build();
    }

    /**
     * 사주 API 통신 장애 시 제공하는 고품질 기본 운세 모델
     */
    private TodayFortuneDTO buildFallbackFortune(String userNickname, Long userNum) {
        List<RecruitmentDTO> fallbackRecruits = fetchRecommendRecruitments("개발", userNum, 6);

        return TodayFortuneDTO.builder()
                .fortuneDate(LocalDate.now().toString())
                .userNickname(userNickname)
                .dayMaster("갑목(甲木) - 곧게 뻗어나가는 거목")
                .dayMasterElement("목")
                .todayIlju("갑자(甲子)일")
                .todayElement("목 / 수")
                .tenGodsRelation("정관")
                .tenGodsMeaning("합격의 문이 활짝 열리고 공신력 있는 조직 및 우수 기업과의 인연이 강하게 닿는 기운입니다.")
                .overallScore(95)
                .overallSummary("오늘은 당신의 실력과 성실함이 공정하게 인정받는 길한 날입니다. 차분히 준비해 온 포트폴리오를 점검하고 적극적으로 채용 공고에 지원해보세요.")
                .advice("망설였던 주요 기업이나 공채 전형에 주저 없이 적극적으로 지원서를 제출하세요.")
                .luckyJobName("소프트웨어 개발자")
                .luckyKeyword("당당한 최종 합격운")
                .luckyColor("신뢰감을 주는 딥 네이비")
                .luckyDirection("북쪽 (차분하고 집중하기 좋은 서재)")
                .luckyNumber(7)
                .sinsalName("장성살")
                .sinsalAdvice("만인을 이끄는 리더십과 카리스마가 돋보입니다. 면접에서 주도적인 태도로 답변을 이끌어보세요.")
                .recommendRecruitments(fallbackRecruits)
                .build();
    }

    /**
     * 내부 십성 해석용 VO
     */
    private static class TenGodsInterpretation {
        final String meaning;
        final int score;
        final String summary;
        final String advice;
        final String luckyJobDisplay;
        final String luckyJobKeyword;
        final String luckyKeyword;

        TenGodsInterpretation(String meaning, int score, String summary, String advice,
                               String luckyJobDisplay, String luckyJobKeyword, String luckyKeyword) {
            this.meaning = meaning;
            this.score = score;
            this.summary = summary;
            this.advice = advice;
            this.luckyJobDisplay = luckyJobDisplay;
            this.luckyJobKeyword = luckyJobKeyword;
            this.luckyKeyword = luckyKeyword;
        }
    }

    /**
     * 내부 오행 해석용 VO
     */
    private static class ElementFortune {
        final String color;
        final String direction;
        final int number;

        ElementFortune(String color, String direction, int number) {
            this.color = color;
            this.direction = direction;
            this.number = number;
        }
    }
}
