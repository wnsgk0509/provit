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
	BAD_REQUEST(220, "잘못된 요청입니다."), INTERNAL_SERVER_ERROR(221, "서버 오류가 발생했습니다."),

	// 3xx 로그인/회원가입 및 인증
	AUTH_LOGIN_SUCCESS(300, "로그인에 성공하였습니다."),
	AUTH_SIGNUP_SUCCESS(301, "회원가입이 완료되었습니다."),
	AUTH_LOGOUT_SUCCESS(302, "로그아웃되었습니다."),
	AUTH_EMAIL_AVAILABLE(303, "사용 가능한 이메일입니다."),
	AUTH_NICKNAME_AVAILABLE(304, "사용 가능한 닉네임입니다."),
	AUTH_CODE_SENT(305, "인증번호가 발송되었습니다. 이메일을 확인해 주세요."),
	AUTH_CODE_VERIFIED(306, "이메일 인증이 완료되었습니다."),
	AUTH_LOGIN_FAILED(310, "이메일 또는 비밀번호가 일치하지 않습니다."),
	AUTH_EMAIL_DUPLICATE(311, "이미 등록된 이메일입니다."),
	AUTH_NICKNAME_DUPLICATE(312, "이미 사용 중인 닉네임입니다."),
	AUTH_CODE_MISMATCH(313, "인증번호가 일치하지 않습니다."),
	AUTH_CODE_EXPIRED(314, "인증번호가 만료되었습니다. 다시 요청해 주세요."),
	AUTH_EMAIL_NOT_VERIFIED(315, "이메일 인증을 먼저 완료해 주세요."),
	AUTH_TOKEN_INVALID(316, "유효하지 않거나 변조된 토큰입니다."),
	AUTH_TOKEN_EXPIRED(317, "토큰이 만료되었습니다. 다시 로그인해 주세요."),
	AUTH_UNAUTHORIZED(318, "인증 자격 증명이 누락되었거나 유효하지 않습니다."),
	AUTH_USER_NOT_FOUND(319, "존재하지 않는 회원입니다."),
	AUTH_MAIL_SEND_FAILED(320, "인증 메일 발송에 실패했습니다. 관리자에게 문의해 주세요."),
	AUTH_ACCOUNT_DELETED(321, "탈퇴 처리된 계정입니다.");

	private final int code;
	private final String message;

	ResponseCode(int code, String message) {
		this.code = code;
		this.message = message;
	}
}
