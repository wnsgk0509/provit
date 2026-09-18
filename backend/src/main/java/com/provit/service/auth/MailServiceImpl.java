package com.provit.service.auth;

import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스 구현체
 */
@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${mail.username:}")
    private String fromEmail;

    @Value("${mail.password:}")
    private String mailPassword;

    @Value("${mail.sender.name:Provit}")
    private String senderName;

    @Autowired
    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void validateMailConfiguration() {
        if (fromEmail == null || fromEmail.trim().isEmpty() || mailPassword == null || mailPassword.trim().isEmpty()) {
            log.warn("⚠️ [MailService] SMTP 발신 계정(mail.username / mail.password)이 설정되지 않았습니다. 개발/테스트 모드로 동작하며 콘솔창에 인증코드가 출력됩니다.");
        } else {
            log.info("✅ [MailService] SMTP 발신 계정({})이 정상 설정되었습니다.", fromEmail);
        }
    }

    private boolean isMailConfigured() {
        return fromEmail != null && !fromEmail.trim().isEmpty() && mailPassword != null && !mailPassword.trim().isEmpty();
    }

    @Override
    public boolean sendVerificationCode(String toEmail, String code) {
        if (!isMailConfigured()) {
            log.warn(">> [DEV MODE] SMTP 미설정 상태: 인증번호를 콘솔에 출력합니다.");
            System.out.println("\n=======================================================");
            System.out.println("  [DEV MODE] 회원가입 이메일 인증번호 모의 발송");
            System.out.println("  - 수신 이메일: " + toEmail);
            System.out.println("  - 인증 코드: [" + code + "]");
            System.out.println("  (실제 이메일 발송은 api.properties에 mail.username/mail.password를 등록하세요)");
            System.out.println("=======================================================\n");
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(fromEmail, senderName, StandardCharsets.UTF_8.name()));
            helper.setTo(toEmail);
            helper.setSubject("[Provit] 회원가입 이메일 인증번호 안내");

            String htmlContent = "<div style='max-width:600px;margin:20px auto;padding:30px;border:1px solid #e0e0e0;border-radius:12px;font-family:sans-serif;background-color:#ffffff;'>"
                    + "<div style='text-align:center;margin-bottom:24px;'>"
                    + "<h1 style='color:#0d6efd;margin:0;font-size:28px;font-weight:bold;'>Provit</h1>"
                    + "<p style='color:#6c757d;font-size:14px;margin-top:6px;'>AI 모의 면접 및 맞춤형 취업 지원 플랫폼</p>"
                    + "</div>"
                    + "<h2 style='font-size:18px;color:#212529;margin-bottom:16px;'>이메일 인증을 완료해 주세요</h2>"
                    + "<p style='color:#495057;font-size:15px;line-height:1.6;'>안녕하세요. Provit에 가입해 주셔서 감사합니다.<br/>아래의 6자리 인증번호를 회원가입 화면에 입력해 주세요.</p>"
                    + "<div style='text-align:center;margin:30px 0;'>"
                    + "<div style='display:inline-block;background-color:#f1f5f9;color:#0d6efd;font-size:32px;font-weight:bold;letter-spacing:6px;padding:16px 36px;border-radius:8px;border:1px dashed #cbd5e1;'>"
                    + code
                    + "</div>"
                    + "</div>"
                    + "<p style='color:#dc3545;font-size:13px;'>* 인증번호는 발송 후 5분간 유효합니다.<br/>* 본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>"
                    + "<hr style='border:none;border-top:1px solid #eeeeee;margin:24px 0;'/>"
                    + "<p style='color:#adb5bd;font-size:12px;text-align:center;margin:0;'>본 메일은 발신 전용이며 문의사항은 고객센터를 이용해 주시기 바랍니다.<br/>© 2026 Provit. All rights reserved.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
            // 인증번호 및 민감정보는 절대 로그에 남기지 않고 대상 수신자만 로깅
            log.info("이메일 인증코드 발송 성공: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("이메일 인증코드 발송 실패 (수신자: {}): {}", toEmail, e.getMessage());
            return false;
        }
    }
}
