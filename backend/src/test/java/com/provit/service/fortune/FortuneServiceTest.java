package com.provit.service.fortune;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.provit.client.fortune.SazuApiClient;
import com.provit.dao.auth.UserDAO;
import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.fortune.TodayFortuneDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.service.fortune.impl.FortuneServiceImpl;

public class FortuneServiceTest {

    private FortuneService fortuneService;
    private SazuApiClient sazuApiClient;
    private UserDAO userDAO;
    private RecruitmentDAO recruitmentDAO;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("api.properties")) {
            if (is != null) {
                properties.load(is);
            }
        }
        String apiKey = properties.getProperty("api.fortune.key", "");
        sazuApiClient = new SazuApiClient(apiKey);

        // UserDAO Mock 생성 (Java Dynamic Proxy)
        userDAO = (UserDAO) Proxy.newProxyInstance(
                UserDAO.class.getClassLoader(),
                new Class<?>[]{UserDAO.class},
                (proxy, method, args) -> {
                    if ("selectByUserNum".equals(method.getName())) {
                        Long userNum = (Long) args[0];
                        if (userNum != null && userNum == 1L) {
                            UserDTO user = new UserDTO();
                            user.setUserNum(1L);
                            user.setUserNickname("개발꿈나무");
                            // 1998년 5월 19일
                            user.setUserBirthDate(new Date(895536000000L));
                            return user;
                        }
                    }
                    return null;
                }
        );

        // RecruitmentDAO Mock 생성 (키워드 2건 + 핫 공고 보충으로 6건 보장 검증)
        recruitmentDAO = (RecruitmentDAO) Proxy.newProxyInstance(
                RecruitmentDAO.class.getClassLoader(),
                new Class<?>[]{RecruitmentDAO.class},
                (proxy, method, args) -> {
                    if ("selectRecruitmentsByKeyword".equals(method.getName())) {
                        List<RecruitmentDTO> list = new ArrayList<>();
                        list.add(RecruitmentDTO.builder().recruitmentNum(101L).title("백엔드 주니어 개발자 채용").companyName("테크기업A").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(102L).title("서버/백엔드 엔지니어 채용").companyName("테크기업B").build());
                        return list;
                    }
                    if ("selectHotRecruitments".equals(method.getName())) {
                        List<RecruitmentDTO> list = new ArrayList<>();
                        list.add(RecruitmentDTO.builder().recruitmentNum(101L).title("백엔드 주니어 개발자 채용 (중복)").companyName("테크기업A").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(201L).title("인기 플랫폼 개발자").companyName("테크기업C").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(202L).title("신입/경력 개발자").companyName("테크기업D").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(203L).title("프론트엔드/웹 개발자").companyName("테크기업E").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(204L).title("풀스택 개발자 채용").companyName("테크기업F").build());
                        list.add(RecruitmentDTO.builder().recruitmentNum(205L).title("데이터 엔지니어 채용").companyName("테크기업G").build());
                        return list;
                    }
                    return null;
                }
        );

        fortuneService = new FortuneServiceImpl(sazuApiClient, userDAO, recruitmentDAO);
    }

    @Test
    public void testGetTodayFortune_Success() {
        // Given: 회원 번호 1L
        Long userNum = 1L;

        // When
        TodayFortuneDTO fortune = fortuneService.getTodayFortune(userNum);

        // Then
        assertNotNull("운세 DTO는 null이 아니어야 합니다", fortune);
        assertEquals("사용자 닉네임이 매핑되어야 합니다", "개발꿈나무", fortune.getUserNickname());
        assertNotNull("운세 날짜가 존재해야 합니다", fortune.getFortuneDate());
        assertNotNull("일간 정보가 존재해야 합니다", fortune.getDayMaster());
        assertNotNull("오늘의 일진이 존재해야 합니다", fortune.getTodayIlju());
        assertNotNull("십성 관계가 존재해야 합니다", fortune.getTenGodsRelation());
        assertNotNull("십성 의미가 존재해야 합니다", fortune.getTenGodsMeaning());
        assertTrue("총점은 80점 이상이어야 합니다", fortune.getOverallScore() >= 80);
        assertNotNull("총평이 존재해야 합니다", fortune.getOverallSummary());
        assertNotNull("행동 조언이 존재해야 합니다", fortune.getAdvice());
        assertNotNull("행운 직무가 존재해야 합니다", fortune.getLuckyJobName());
        assertNotNull("행운 색상이 존재해야 합니다", fortune.getLuckyColor());
        assertNotNull("행운 방향이 존재해야 합니다", fortune.getLuckyDirection());
        assertTrue("행운 숫자는 1 이상이어야 합니다", fortune.getLuckyNumber() >= 1);
        assertNotNull("신살 명칭이 존재해야 합니다", fortune.getSinsalName());
        assertNotNull("신살 조언이 존재해야 합니다", fortune.getSinsalAdvice());

        // 채용 공고 6건 보장 및 중복 제거 검증
        assertNotNull("추천 채용 공고 목록이 존재해야 합니다", fortune.getRecommendRecruitments());
        assertEquals("추천 공고는 중복 없이 총 6건이어야 합니다", 6, fortune.getRecommendRecruitments().size());
        assertEquals(Long.valueOf(101L), fortune.getRecommendRecruitments().get(0).getRecruitmentNum());
        assertEquals(Long.valueOf(102L), fortune.getRecommendRecruitments().get(1).getRecruitmentNum());
        assertEquals(Long.valueOf(201L), fortune.getRecommendRecruitments().get(2).getRecruitmentNum());

        System.out.println("====== [FortuneService 테스트 성공 결과] ======");
        System.out.println("닉네임: " + fortune.getUserNickname());
        System.out.println("일간: " + fortune.getDayMaster() + " (" + fortune.getDayMasterElement() + ")");
        System.out.println("오늘 일진: " + fortune.getTodayIlju() + " (" + fortune.getTodayElement() + ")");
        System.out.println("십성 관계: " + fortune.getTenGodsRelation() + " -> " + fortune.getTenGodsMeaning());
        System.out.println("취업 총점: " + fortune.getOverallScore() + "점");
        System.out.println("총평: " + fortune.getOverallSummary());
        System.out.println("행동 조언: " + fortune.getAdvice());
        System.out.println("행운 직무: " + fortune.getLuckyJobName());
        System.out.println("행운 아이템: 색상=" + fortune.getLuckyColor() + ", 방향=" + fortune.getLuckyDirection() + ", 숫자=" + fortune.getLuckyNumber());
        System.out.println("신살: " + fortune.getSinsalName() + " (" + fortune.getSinsalAdvice() + ")");
        System.out.println("추천 공고 수: " + fortune.getRecommendRecruitments().size() + "건");
    }

    @Test
    public void testGetTodayFortune_GuestUser() {
        // Given: 미로그인 (userNum == null)
        Long userNum = null;

        // When
        TodayFortuneDTO fortune = fortuneService.getTodayFortune(userNum);

        // Then
        assertNotNull("운세 DTO는 null이 아니어야 합니다", fortune);
        assertEquals("미로그인 시 기본 닉네임은 '취준생'이어야 합니다", "취준생", fortune.getUserNickname());
        assertNotNull("기본 일간이 매핑되어야 합니다", fortune.getDayMaster());
        assertEquals("추천 공고는 6건이어야 합니다", 6, fortune.getRecommendRecruitments().size());
    }
}
