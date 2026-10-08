package com.provit.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.response.ApiResponse;
import com.provit.service.TestService;

/**
 * 프론트엔드 연동 및 서버 헬스체크 테스트용 컨트롤러
 * (순수 new 생성자 방식 적용 예시)
 */
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final TestService testService;

    // 1. 스프링 빈 주입도 생성자 주입(Constructor Injection) 적용
    @Autowired
    public HealthCheckController(@Autowired(required = false) TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> data = null;
        if (testService != null) {
            data = testService.getSystemStatus();
        }

        // 2. new 생성자 방식으로 ApiResponse 및 ResponseEntity 반환
        ApiResponse<Map<String, Object>> response = new ApiResponse<>(ResponseCode.SUCCESS, data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
