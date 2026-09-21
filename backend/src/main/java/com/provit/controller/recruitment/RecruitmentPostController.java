package com.provit.controller.recruitment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.dto.response.PageResponse;
import com.provit.service.recruitment.RecruitmentService;

@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentPostController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentPostController.class);

    private final RecruitmentService recruitmentService;

    @Autowired
    public RecruitmentPostController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    /**
     * 채용 공고 페이징 & 필터링 목록 조회 API
     * 예: GET /api/recruitment?page=1&size=10&keyword=네이버&location=서울
     */
    @GetMapping
    public ApiResponse<PageResponse<RecruitmentDTO>> getRecruitmentList(@ModelAttribute RecruitmentSearchDTO searchDTO) {
        log.info(">> [/api/recruitment] 채용 공고 목록 조회 요청: {}", searchDTO);

        PageResponse<RecruitmentDTO> pageResult = recruitmentService.getRecruitmentList(searchDTO);
        return ApiResponse.success(pageResult);
    }

    /**
     * 사람인 실시간 인기 상위 공고 수동 크롤링 & DB 동기화 트리거 API
     * 예: POST /api/recruitment/sync?limit=100 (테스트 시 limit=20 등으로 조절 가능, 기본 1,000)
     */
    @PostMapping("/sync")
    public ApiResponse<String> syncRecruitments(
            @RequestParam(value = "limit", defaultValue = "1000") int limit) {
        log.info(">> [/api/recruitment/sync] 채용 공고 수동 동기화 요청 수신 (목표 상한: {}건)", limit);

        int syncedCount = recruitmentService.syncSaraminRecruitments(limit);
        return ApiResponse.success("사람인 실시간 인기 공고 총 " + syncedCount + "건이 성공적으로 수집/동기화되었습니다.");
    }
}
