package com.provit.controller.fortune;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.annotation.LoginUser;
import com.provit.common.auth.AuthCookieService;
import com.provit.dto.fortune.TodayFortuneDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.fortune.FortuneService;
import com.provit.util.jwt.JwtProvider;

/**
 * 오늘의 취업 운세 및 행운 추천 공고 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/fortune")
public class FortuneController {

    private static final Logger log = LoggerFactory.getLogger(FortuneController.class);

    private final FortuneService fortuneService;
    private final AuthCookieService authCookieService;
    private final JwtProvider jwtProvider;

    @Autowired
    public FortuneController(FortuneService fortuneService,
                             AuthCookieService authCookieService,
                             JwtProvider jwtProvider) {
        this.fortuneService = fortuneService;
        this.authCookieService = authCookieService;
        this.jwtProvider = jwtProvider;
    }

    /**
     * 오늘의 취업 운세 및 맞춤 추천 공고 조회 API
     * 
     * GET /api/fortune/today
     * 
     * - 로그인 회원: 본인의 생년월일 사주 분석 및 스크랩 여부가 반영된 추천 공고 6건 반환
     * - 미로그인 게스트: 표준 사주 분석 및 최신 추천 공고 6건 반환
     * 
     * @param userNum 로그인한 사용자 고유 번호 (@LoginUser 주입)
     * @param request HTTP 서블릿 요청 객체 (쿠키 안전 폴백)
     * @return ApiResponse<TodayFortuneDTO>
     */
    @GetMapping("/today")
    public ApiResponse<TodayFortuneDTO> getTodayFortune(
            @LoginUser Long userNum,
            HttpServletRequest request) {

        // LoginUserArgumentResolver로 주입되지 않은 경우 쿠키 직접 조회 (안전 폴백)
        if (userNum == null && request != null) {
            userNum = getAuthenticatedUserNum(request);
        }

        log.info(">> [/api/fortune/today] 오늘의 취업 운세 요청 수신: userNum={}", userNum);

        TodayFortuneDTO fortuneDTO = fortuneService.getTodayFortune(userNum);
        return ApiResponse.success(fortuneDTO);
    }

    /**
     * 쿠키의 AccessToken으로부터 인증된 userNum 추출
     */
    private Long getAuthenticatedUserNum(HttpServletRequest request) {
        if (request == null) return null;
        try {
            String token = authCookieService.getAccessToken(request).orElse(null);
            if (token == null) return null;
            if (!jwtProvider.validateToken(token)) return null;
            return jwtProvider.getUserNum(token);
        } catch (Exception e) {
            log.warn(">> [FortuneController] 토큰 파싱 중 예외: {}", e.getMessage());
            return null;
        }
    }
}
