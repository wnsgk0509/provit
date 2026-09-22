package com.provit.controller.recruitment;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.dto.response.PageResponse;
import com.provit.service.recruitment.RecruitmentService;
import com.provit.util.jwt.JwtProvider;

@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentPostController {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentPostController.class);

    private final RecruitmentService recruitmentService;
    private final JwtProvider jwtProvider;

    @Autowired
    public RecruitmentPostController(RecruitmentService recruitmentService, JwtProvider jwtProvider) {
        this.recruitmentService = recruitmentService;
        this.jwtProvider = jwtProvider;
    }

    /**
     * 채용 공고 페이징 & 필터링 목록 조회 API (일반 사용자 공개)
     * 예: GET /api/recruitment?page=1&size=10&keyword=네이버&location=서울
     */
    @GetMapping
    public ApiResponse<PageResponse<RecruitmentDTO>> getRecruitmentList(@ModelAttribute RecruitmentSearchDTO searchDTO) {
        log.info(">> [/api/recruitment] 채용 공고 목록 조회 요청: {}", searchDTO);

        PageResponse<RecruitmentDTO> pageResult = recruitmentService.getRecruitmentList(searchDTO);
        return ApiResponse.success(pageResult);
    }

    /**
     * 사람인 실시간 인기 상위 공고 수동 크롤링 & DB 동기화 트리거 API (관리자 전용)
     * 예: POST /api/recruitment/sync?limit=100 (기본 1,000)
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncRecruitments(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "1000") int limit) {
        log.info(">> [/api/recruitment/sync] 채용 공고 수동 동기화 요청 수신 (목표 상한: {}건)", limit);

        if (!isAdmin(request)) {
            log.warn(">> [/api/recruitment/sync] 미인가 접근 차단 (관리자 권한 없음)");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, "관리자(ADMIN) 권한이 필요한 요청입니다."));
        }

        int syncedCount = recruitmentService.syncSaraminRecruitments(limit);
        return ResponseEntity.ok(ApiResponse.success("사람인 실시간 인기 공고 총 " + syncedCount + "건이 성공적으로 수집/동기화되었습니다."));
    }

    /**
     * 마감일이 지난 공고 수동 비활성화(IS_ACTIVE=0) 트리거 API (관리자 전용)
     * 예: POST /api/recruitment/expire
     */
    @PostMapping("/expire")
    public ResponseEntity<ApiResponse<String>> deactivateExpiredRecruitments(HttpServletRequest request) {
        log.info(">> [/api/recruitment/expire] 마감 공고 수동 비활성화 요청 수신");

        if (!isAdmin(request)) {
            log.warn(">> [/api/recruitment/expire] 미인가 접근 차단 (관리자 권한 없음)");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, "관리자(ADMIN) 권한이 필요한 요청입니다."));
        }

        int count = recruitmentService.deactivateExpiredRecruitments();
        return ResponseEntity.ok(ApiResponse.success("마감일이 지난 공고 총 " + count + "건이 비활성화 처리되었습니다."));
    }

    /**
     * 요청 헤더의 JWT 토큰을 추출하여 ADMIN(관리자) 권한 보유 여부를 검증합니다.
     */
    private boolean isAdmin(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            return false;
        }

        String role = jwtProvider.getUserRole(token);
        return "ADMIN".equalsIgnoreCase(role);
    }
}
