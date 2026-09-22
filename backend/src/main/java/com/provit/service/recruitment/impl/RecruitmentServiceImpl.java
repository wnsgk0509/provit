package com.provit.service.recruitment.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.response.PageResponse;
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
    @Transactional
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
}
