package com.provit.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

/**
 * 공통 응답 상태 코드 및 헤더 정보 Enum
 * 
 * @JsonFormat을 적용하여 JSON 변환 시 code(200)와 message가 함께 직렬화됩니다: "responseCode": {
 *              "code": 200, "message": "요청이 성공적으로 처리되었습니다." }
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ResponseCode {
	// 2xx 성공 + 성공했지만 empty 데이터
	SUCCESS(200, "요청이 성공적으로 처리되었습니다."), SUCCESS_EMPTY(201, "요청이 성공적으로 처리되었습니다."), CREATED(202, "리소스가 성공적으로 생성되었습니다."),
	BAD_REQUEST(220, "abc"), INTERNAL_SERVER_ERROR(221, "def");
	// 3xx 로그인/회원가입

	// 4xx 채용공고

	// 5xx 모의면접

	// 6xx 운세, MBTI

	// 7xx 커뮤니티

	;

	private final int code;
	private final String message;

	ResponseCode(int code, String message) {
		this.code = code;
		this.message = message;
	}
}
