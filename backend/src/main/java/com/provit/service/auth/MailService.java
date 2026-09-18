package com.provit.service.auth;

/**
 * 이메일 발송 서비스 인터페이스
 */
public interface MailService {

    /**
     * 인증번호 이메일 발송
     * 
     * @param toEmail 수신자 이메일 주소
     * @param code    6자리 인증번호
     * @return 발송 성공 여부
     */
    boolean sendVerificationCode(String toEmail, String code);
}
