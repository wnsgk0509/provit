package com.provit.dto.response;

import com.provit.common.ResponseCode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 프론트엔드 통신용 공통 JSON 응답 규격 (생성자 기반 간결한 구조)
 * 
 * 응답 예시:
 * {
 *   "responseCode": {
 *     "code": 200,
 *     "message": "요청이 성공적으로 처리되었습니다."
 *   },
 *   "data": { ... }
 * }
 */
@Getter
@NoArgsConstructor
@ToString
public class ApiResponse<T> {

    // 1. 헤더 역할 (ResponseCode 기반 정보)
    private ResponseCode responseCode;

    // 2. 바디 데이터 영역
    private T data;

    // ==========================================
    // 🔨 생성자 (Constructor)
    // ==========================================

    /**
     * responseCode와 data를 모두 받는 전체 생성자
     */
    public ApiResponse(ResponseCode responseCode, T data) {
        this.responseCode = responseCode;
        this.data = data;
    }

    /**
     * data 없이 responseCode만 받는 생성자 (단순 성공/에러 처리용)
     */
    public ApiResponse(ResponseCode responseCode) {
        this(responseCode, null);
    }

    // ==========================================
    // 🟢 성공(Success) 응답 메서드
    // ==========================================

    /**
     * 1. 데이터가 있는 기본 성공 응답 (ResponseCode.SUCCESS 자동 세팅)
     * 사용 예: ApiResponse.success(userList);
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, data);
    }

    /**
     * 2. 반환할 데이터가 없는 단순 성공 응답 (ResponseCode.SUCCESS)
     * 사용 예: ApiResponse.success(); (삭제, 상태변경 등)
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS, null);
    }

    /**
     * 3. 특정 성공 코드(예: CREATED 201)와 데이터를 함께 반환
     * 사용 예: ApiResponse.success(ResponseCode.CREATED, newPost);
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return new ApiResponse<>(responseCode, data);
    }

    // ==========================================
    // 🔴 에러(Error) 응답 메서드
    // ==========================================

    /**
     * 1. 에러 ResponseCode만 반환 (data는 null)
     * 사용 예: ApiResponse.error(ResponseCode.NOT_FOUND);
     *          ApiResponse.error(ResponseCode.UNAUTHORIZED);
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode, null);
    }

    /**
     * 2. 에러 ResponseCode와 에러 상세 데이터(검증 실패 필드 등)를 함께 반환
     * 사용 예: ApiResponse.error(ResponseCode.BAD_REQUEST, validationErrors);
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, T errorData) {
        return new ApiResponse<>(responseCode, errorData);
    }

    // ==========================================
    // ⚪ 범용(of) 생성 메서드
    // ==========================================

    /**
     * ResponseCode와 데이터를 직접 조합하여 생성
     */
    public static <T> ApiResponse<T> of(ResponseCode responseCode, T data) {
        return new ApiResponse<>(responseCode, data);
    }

    /**
     * ResponseCode만 전달하여 생성
     */
    public static <T> ApiResponse<T> of(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode, null);
    }
}
