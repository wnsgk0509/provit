package com.provit.service.recruitment.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.JobScrapResponseDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.recruitment.UserRecommendResponseDTO;
import com.provit.dto.response.PageResponse;
import com.provit.dto.user.UserJobPreferenceDTO;
import com.provit.service.recruitment.RecruitmentService;
import com.provit.util.SaraminCrawler;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentServiceImpl.class);

    private final RecruitmentDAO recruitmentDAO;
    private final SaraminCrawler saraminCrawler;

    @Autowired
    public RecruitmentServiceImpl(RecruitmentDAO recruitmentDAO, SaraminCrawler saraminCrawler) {
        this.recruitmentDAO = recruitmentDAO;
        this.saraminCrawler = saraminCrawler;
    }

    @Override
    public List<OccupationDTO> getSarmainOccupationInfo() {
        log.info(">> [Service] 직군 목록 조회 (T_OCCUPATION)");
        return recruitmentDAO.selectOccupationList();
    }

    @Override
    public List<JobDTO> getJobListByOccupation(String occupationCode) {
        log.info(">> [Service] 직무 목록 조회 (T_JOB) - 직군코드: {}", occupationCode);
        return recruitmentDAO.selectJobListByOccupation(occupationCode);
    }

    @Override
    public int syncSaraminRecruitments(int limit) {
        log.info(">> [Service] 사람인 실시간 인기 공고 크롤링 동기화 시작 (요청 상한: {}건)", limit);

        List<RecruitmentDTO> recruitments = saraminCrawler.crawlTopRecruitments(limit);
        log.info(">> [Service] 크롤러 수집 완료: 총 {}건 파싱됨", recruitments.size());

        int successCount = 0;
        for (RecruitmentDTO dto : recruitments) {
            try {
                recruitmentDAO.mergeRecruitment(dto);
                successCount++;
            } catch (Exception e) {
                log.warn(">> [Service] 공고(ID: {}) 적재 중 오류 발생 (스킵): {}", dto.getSaraminJobId(), e.getMessage());
            }
        }

        int expiredCount = recruitmentDAO.deactivateExpiredRecruitments();
        log.info(">> [Service] 공고 동기화 완료: {}건 저장/갱신, {}건 만료 비활성화", successCount, expiredCount);

        return successCount;
    }

    @Override
    public PageResponse<RecruitmentDTO> getRecruitmentList(RecruitmentSearchDTO searchDTO) {
        log.info(">> [Service] 채용 공고 목록 페이징 조회: {}", searchDTO);

        long totalCount = recruitmentDAO.selectRecruitmentCount(searchDTO);
        List<RecruitmentDTO> list = recruitmentDAO.selectRecruitmentList(searchDTO);

        return PageResponse.of(list, searchDTO.getPage(), searchDTO.getSize(), totalCount);
    }

    @Override
    @Transactional
    public int deactivateExpiredRecruitments() {
        log.info(">> [Service] 마감일 경과 채용 공고 일괄 비활성화(IS_ACTIVE=0) 시작");
        int deactivatedCount = recruitmentDAO.deactivateExpiredRecruitments();
        log.info(">> [Service] 마감일 경과 채용 공고 일괄 비활성화 완료: 총 {}건 비활성화 처리", deactivatedCount);
        return deactivatedCount;
    }

    @Override
    @Transactional
    public JobScrapResponseDTO toggleJobScrap(long recruitmentNum, long userNum) {
        log.info(">> [Service] 관심 공고 스크랩 토글 요청: recruitmentNum={}, userNum={}", recruitmentNum, userNum);

        // 1. Delete-First: 사전 조회(SELECT) 없이 먼저 삭제를 시도하여 존재 여부 판별
        int deleted = recruitmentDAO.deleteJobScrap(recruitmentNum, userNum);
        boolean isScrapped;
        String message;

        if (deleted > 0) {
            isScrapped = false;
            message = "관심 공고에서 제외되었습니다.";
            log.info(">> [Service] 스크랩 취소 완료 (삭제 성공): recruitmentNum={}, userNum={}", recruitmentNum, userNum);
        } else {
            // 2. 존재하지 않았으므로 신규 등록(INSERT) 시도
            try {
                recruitmentDAO.insertJobScrap(recruitmentNum, userNum);
                isScrapped = true;
                message = "관심 공고로 등록되었습니다.";
                log.info(">> [Service] 스크랩 등록 완료 (신규 삽입): recruitmentNum={}, userNum={}", recruitmentNum, userNum);
            } catch (DuplicateKeyException e) {
                // 3. 오직 '동일 사용자·공고의 중복 키(PK) 충돌'인 경우에만 동시성 경합으로 흡수하여 정상 반환 (멱등성 보장)
                log.warn(">> [Service] 스크랩 동시 요청 경합 감지 (PK 중복 키 충돌 흡수): recruitmentNum={}, userNum={}", recruitmentNum, userNum);
                isScrapped = true;
                message = "관심 공고로 등록되었습니다.";
            } catch (DataIntegrityViolationException e) {
                // 4. 존재하지 않는 공고/사용자에 대한 외래키(FK) 위반, NOT NULL 위반 등 기타 무결성 오류는 예외 발생 처리
                log.error(">> [Service] 스크랩 등록 실패 (외래키 또는 데이터 무결성 위반): recruitmentNum={}, userNum={}", recruitmentNum, userNum);
                throw new IllegalArgumentException("존재하지 않는 채용 공고이거나 유효하지 않은 요청입니다.");
            }
        }

        return JobScrapResponseDTO.builder()
                .recruitmentNum(recruitmentNum)
                .userNum(userNum)
                .isScrapped(isScrapped)
                .message(message)
                .build();
    }

    @Override
    public UserRecommendResponseDTO getUserJobRecommendations(Long userNum) {
        log.info(">> [Service] 사용자 맞춤 채용 공고 추천 요청 수신 (userNum: {})", userNum);

        String targetJobName = null;
        String targetOccupationName = null;
        String userNickname = null;

        if (userNum != null) {
            // [1단계]: 현재 T_USER 기준 희망 직무 및 닉네임 조회
            // (추후 팀원의 T_RESUME 직무 컬럼 머지 시, 최신 이력서 직무 우선 조회 -> 없으면 T_USER 폴백 구조로 확장 가능)
            UserJobPreferenceDTO pref = recruitmentDAO.selectUserJobPreference(userNum);
            if (pref != null) {
                userNickname = pref.getUserNickname();
                if (pref.getJobName() != null && !pref.getJobName().trim().isEmpty()) {
                    targetJobName = pref.getJobName().trim();
                }
                if (pref.getOccupationName() != null && !pref.getOccupationName().trim().isEmpty()) {
                    targetOccupationName = pref.getOccupationName().trim();
                }
            }
        }

        // [2단계 - Tier 1]: 유저의 소분류 직무명 키워드 매칭 시도 (최대 6건)
        if (targetJobName != null) {
            List<RecruitmentDTO> matchedJobs = recruitmentDAO.selectRecruitmentsByKeyword(targetJobName, userNum, 6);
            if (matchedJobs != null && !matchedJobs.isEmpty()) {
                log.info(">> [Service] 유저 직무 키워드('{}') 매칭 성공 (총 {}건 반환)", targetJobName, matchedJobs.size());
                return UserRecommendResponseDTO.builder()
                        .recommendType("JOB_MATCH")
                        .targetJobName(targetJobName)
                        .userNickname(userNickname)
                        .recruitments(matchedJobs)
                        .build();
            }
        }

        // [2단계 - Tier 2]: 소분류 매칭 결과가 없거나 직무코드가 없고 직군코드만 있는 경우, 대분류 직군명 키워드 매칭
        if (targetOccupationName != null) {
            // "IT개발·데이터" -> "IT개발", "기획·전략" -> "기획" 등 특수문자/구분기호 앞단 핵심 키워드 정제
            String cleanOccName = targetOccupationName.split("[·/,]")[0].trim();
            List<RecruitmentDTO> matchedOccJobs = recruitmentDAO.selectRecruitmentsByKeyword(cleanOccName, userNum, 6);
            if (matchedOccJobs != null && !matchedOccJobs.isEmpty()) {
                log.info(">> [Service] 유저 직군 키워드('{}') 매칭 성공 (총 {}건 반환)", cleanOccName, matchedOccJobs.size());
                return UserRecommendResponseDTO.builder()
                        .recommendType("OCCUPATION_MATCH")
                        .targetJobName(targetOccupationName)
                        .userNickname(userNickname)
                        .recruitments(matchedOccJobs)
                        .build();
            }
        }

        // [2단계 - Tier 3 Fallback]: 비로그인, 직무 미설정, 혹은 매칭 공고가 없을 때 실시간 인기/최신 공고 반환
        List<RecruitmentDTO> hotJobs = recruitmentDAO.selectHotRecruitments(userNum, 6);
        log.info(">> [Service] 매칭 결과 부재 또는 미설정으로 인기 공고(Fallback) 반환 (총 {}건)", hotJobs != null ? hotJobs.size() : 0);
        return UserRecommendResponseDTO.builder()
                .recommendType("POPULAR_FALLBACK")
                .targetJobName(null)
                .userNickname(userNickname)
                .recruitments(hotJobs != null ? hotJobs : java.util.Collections.emptyList())
                .build();
    }
}
