package com.provit.service.recruitment;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.UserRecommendResponseDTO;
import com.provit.dto.user.UserJobPreferenceDTO;
import com.provit.service.document.MainResumeService;
import com.provit.service.recruitment.impl.RecruitmentServiceImpl;

public class RecruitmentRecommendTest {

    private RecruitmentService recruitmentService;
    private RecruitmentDAO recruitmentDAO;
    private MainResumeService mainResumeService;

    // 테스트 제어용 상태
    private MainResumeJobInfoDTO mockMainResume;
    private List<RecruitmentDTO> mockKeywordRecruitments;
    private List<RecruitmentDTO> mockHotRecruitments;

    @Before
    public void setUp() {
        mockMainResume = null;
        mockKeywordRecruitments = new ArrayList<>();
        mockHotRecruitments = new ArrayList<>();

        // RecruitmentDAO Mock
        recruitmentDAO = (RecruitmentDAO) Proxy.newProxyInstance(
                RecruitmentDAO.class.getClassLoader(),
                new Class<?>[]{RecruitmentDAO.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("selectUserJobPreference".equals(methodName)) {
                        UserJobPreferenceDTO pref = new UserJobPreferenceDTO();
                        pref.setUserNum(((Long) args[0]).intValue());
                        pref.setUserNickname("테스트취준생");
                        return pref;
                    }
                    if ("selectRecruitmentsByKeyword".equals(methodName)) {
                        String keyword = (String) args[0];
                        // 키워드에 따라 반환
                        if ("백엔드 개발자".equals(keyword)) {
                            return mockKeywordRecruitments;
                        }
                        if ("IT개발".equals(keyword)) {
                            return mockKeywordRecruitments;
                        }
                        return new ArrayList<RecruitmentDTO>();
                    }
                    if ("selectHotRecruitments".equals(methodName)) {
                        return mockHotRecruitments;
                    }
                    return null;
                }
        );

        // MainResumeService Mock
        mainResumeService = (MainResumeService) Proxy.newProxyInstance(
                MainResumeService.class.getClassLoader(),
                new Class<?>[]{MainResumeService.class},
                (proxy, method, args) -> {
                    if ("getMainResumeJobInfo".equals(method.getName())) {
                        return mockMainResume;
                    }
                    return null;
                }
        );

        recruitmentService = new RecruitmentServiceImpl(recruitmentDAO, null, mainResumeService);
    }

    @Test
    public void testRecommend_Tier1_JobMatch() {
        // Given: 대표 이력서에 직무명 "백엔드 개발자"가 지정되어 있는 경우
        mockMainResume = new MainResumeJobInfoDTO();
        mockMainResume.setResumeNum(10L);
        mockMainResume.setResumeTitle("백엔드 이력서");
        mockMainResume.setOccupationName("IT개발·데이터");
        mockMainResume.setJobName("백엔드 개발자");

        mockKeywordRecruitments.add(RecruitmentDTO.builder().recruitmentNum(101L).title("백엔드 개발자 채용").build());

        // When
        UserRecommendResponseDTO result = recruitmentService.getUserJobRecommendations(1L);

        // Then
        assertNotNull(result);
        assertEquals("JOB_MATCH", result.getRecommendType());
        assertEquals("백엔드 개발자", result.getTargetJobName());
        assertEquals("테스트취준생", result.getUserNickname());
        assertEquals(1, result.getRecruitments().size());
        assertEquals(Long.valueOf(101L), result.getRecruitments().get(0).getRecruitmentNum());
        System.out.println(">> [검증 성공] Tier 1 직무 매칭: " + result.getRecommendType() + ", 직무=" + result.getTargetJobName());
    }

    @Test
    public void testRecommend_Tier2_OccupationMatch() {
        // Given: 직무명 검색 결과가 없어 직군 "IT개발·데이터" -> "IT개발"로 매칭되는 경우
        mockMainResume = new MainResumeJobInfoDTO();
        mockMainResume.setResumeNum(10L);
        mockMainResume.setOccupationName("IT개발·데이터");
        mockMainResume.setJobName("특이직무(공고없음)");

        // 직무명 키워드로는 빈 리스트, IT개발 키워드로 공고 세팅
        mockKeywordRecruitments.add(RecruitmentDTO.builder().recruitmentNum(201L).title("IT개발 신입 채용").build());

        // When
        UserRecommendResponseDTO result = recruitmentService.getUserJobRecommendations(1L);

        // Then
        assertNotNull(result);
        assertEquals("OCCUPATION_MATCH", result.getRecommendType());
        assertEquals("IT개발·데이터", result.getTargetJobName());
        assertEquals(1, result.getRecruitments().size());
        assertEquals(Long.valueOf(201L), result.getRecruitments().get(0).getRecruitmentNum());
        System.out.println(">> [검증 성공] Tier 2 직군 매칭: " + result.getRecommendType() + ", 직군=" + result.getTargetJobName());
    }

    @Test
    public void testRecommend_Tier3_NoMainResume_Fallback() {
        // Given: 대표 이력서가 없는 회원 (mockMainResume = null)
        mockMainResume = null;
        mockHotRecruitments.add(RecruitmentDTO.builder().recruitmentNum(301L).title("실시간 인기 공고").build());

        // When
        UserRecommendResponseDTO result = recruitmentService.getUserJobRecommendations(1L);

        // Then
        assertNotNull(result);
        assertEquals("POPULAR_FALLBACK", result.getRecommendType());
        assertNull(result.getTargetJobName());
        assertEquals("테스트취준생", result.getUserNickname());
        assertEquals(1, result.getRecruitments().size());
        assertEquals(Long.valueOf(301L), result.getRecruitments().get(0).getRecruitmentNum());
        System.out.println(">> [검증 성공] Tier 3 대표 이력서 미등록 Fallback: " + result.getRecommendType());
    }

    @Test
    public void testRecommend_Tier3_GuestUser_Fallback() {
        // Given: 비로그인 (userNum = null)
        mockHotRecruitments.add(RecruitmentDTO.builder().recruitmentNum(301L).title("실시간 인기 공고").build());

        // When
        UserRecommendResponseDTO result = recruitmentService.getUserJobRecommendations(null);

        // Then
        assertNotNull(result);
        assertEquals("POPULAR_FALLBACK", result.getRecommendType());
        assertNull(result.getUserNickname());
        assertEquals(1, result.getRecruitments().size());
        System.out.println(">> [검증 성공] Tier 3 비로그인 Fallback: " + result.getRecommendType());
    }
}
