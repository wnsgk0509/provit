package com.provit.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.provit.dto.response.ApiResponse;

/**
 * 전역 예외 처리기 (new 생성자 방식으로 ApiResponse 및 ResponseEntity 반환)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 잘못된 파라미터 / 유효성 검사 실패 예외 (400 Bad Request)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청 파라미터: {}", e.getMessage());

        // 체이닝(.status().body()) 대신 순수 new 생성자 방식 사용
        ApiResponse<String> response = new ApiResponse<>(ResponseCode.BAD_REQUEST, e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 기타 정의되지 않은 모든 서버 내부 예외 처리 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("서버 내부 오류 발생: ", e);

        // 체이닝(.status().body()) 대신 순수 new 생성자 방식 사용
        ApiResponse<String> response = new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
