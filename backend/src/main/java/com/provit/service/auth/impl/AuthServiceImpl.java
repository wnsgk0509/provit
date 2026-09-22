package com.provit.service.auth.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.common.ResponseCode;
import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.LoginResponseDTO;
import com.provit.dto.auth.MyPageUpdateRequestDTO;
import com.provit.dto.auth.SignupRequestDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.auth.UserResponseDTO;
import com.provit.dto.auth.WithdrawalRequestDTO;
import com.provit.service.auth.AuthService;
import com.provit.service.auth.MailService;
import com.provit.util.CommonUtil;
import com.provit.util.jwt.JwtProvider;

/**
 * 회원가입, 로그인 및 이메일 인증 비즈니스 로직 구현체
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final MailService mailService;

    // 이메일 인증코드 캐시 (이메일 -> 인증코드 정보, 유효시간 5분)
    private final Map<String, VerificationCodeInfo> emailCodeMap = new ConcurrentHashMap<>();
    // 이메일 인증 완료 티켓 캐시 (토큰 -> 이메일 정보, 유효시간 30분)
    private final Map<String, VerificationTokenInfo> verifiedTokenMap = new ConcurrentHashMap<>();
    // 동일 이메일의 인증 메일 반복 발송 방지용 쿨다운
    private final Map<String, Long> emailSendCooldownMap = new ConcurrentHashMap<>();

    private static final long CODE_EXPIRE_MILLIS = 3 * 60 * 1000L; // 3분
    private static final long TOKEN_EXPIRE_MILLIS = 30 * 60 * 1000L; // 30분
    private static final long SEND_COOLDOWN_MILLIS = 60 * 1000L; // 1분
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final int ADULT_AGE = 19;
    // 가입 이름과 서비스 전반에서 사용하는 닉네임의 길이 기준이다.
    private static final int USER_NAME_MAX_LENGTH = 4;
    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=\\S{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*$");

    @Autowired
    public AuthServiceImpl(UserDAO userDAO,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           MailService mailService) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.mailService = mailService;
    }

    private static class VerificationCodeInfo {
        final String code;
        final long expireAt;
        int failedAttempts;

        VerificationCodeInfo(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    private String normalizeEmail(String email) {
        if (CommonUtil.isEmpty(email)) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
        }
        return normalizedEmail;
    }

    private static class VerificationTokenInfo {
        final String email;
        final long expireAt;

        VerificationTokenInfo(String email, long expireAt) {
            this.email = email;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return userDAO.countByEmail(normalizeEmail(email)) == 0;
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        if (CommonUtil.isEmpty(nickname)) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() < NICKNAME_MIN_LENGTH || trimmedNickname.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하로 입력해 주세요.");
        }
        return userDAO.countByNickname(trimmedNickname) == 0;
    }

    @Override
    public void sendVerificationEmail(String email) {
        String trimmedEmail = normalizeEmail(email);

        // 1. 이미 등록된 이메일인지 검증
        if (userDAO.countByEmail(trimmedEmail) > 0) {
            throw new IllegalArgumentException("이미 가입된 이메일 주소입니다.");
        }

        Long nextSendAt = emailSendCooldownMap.get(trimmedEmail);
        long now = System.currentTimeMillis();
        if (nextSendAt != null && nextSendAt > now) {
            long remainingSeconds = (nextSendAt - now + 999) / 1000;
            throw new IllegalStateException("인증번호는 " + remainingSeconds + "초 후에 다시 발송할 수 있습니다.");
        }

        // 2. 6자리 난수 생성 (100000 ~ 999999)
        int randomCode = 100000 + SECURE_RANDOM.nextInt(900000);
        String code = String.valueOf(randomCode);

        // 3. 기존 인증정보가 있다면 덮어써서 최신화 (재전송 시 이전 코드 즉시 무효화)
        long expireAt = now + CODE_EXPIRE_MILLIS;
        emailCodeMap.put(trimmedEmail, new VerificationCodeInfo(code, expireAt));

        // 4. 메일 발송 (코드 및 민감정보는 로그에 남기지 않음)
        boolean sent = mailService.sendVerificationCode(trimmedEmail, code);
        if (!sent) {
            // 발송 실패 시 캐시 삭제하여 잘못된 인증 방지
            emailCodeMap.remove(trimmedEmail);
            throw new IllegalStateException("인증 이메일 발송에 실패했습니다. 이메일 주소를 확인해 주세요.");
        }
        emailSendCooldownMap.put(trimmedEmail, now + SEND_COOLDOWN_MILLIS);
    }

    @Override
    public String verifyEmailCode(String email, String code) {
        if (CommonUtil.isEmpty(email) || CommonUtil.isEmpty(code)) {
            throw new IllegalArgumentException("이메일과 인증번호를 모두 입력해 주세요.");
        }
        String trimmedEmail = normalizeEmail(email);
        String trimmedCode = code.trim();
        if (!trimmedCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("인증번호 6자리를 입력해 주세요.");
        }

        VerificationCodeInfo info = emailCodeMap.get(trimmedEmail);
        if (info == null) {
            throw new IllegalArgumentException("인증번호가 발송되지 않았거나 만료되었습니다. 다시 발송해 주세요.");
        }

        if (info.isExpired()) {
            emailCodeMap.remove(trimmedEmail);
            throw new IllegalArgumentException("인증번호 유효시간(5분)이 만료되었습니다. 다시 발송해 주세요.");
        }

        if (!info.code.equals(trimmedCode)) {
            info.failedAttempts++;
            if (info.failedAttempts >= MAX_VERIFICATION_ATTEMPTS) {
                emailCodeMap.remove(trimmedEmail);
                throw new IllegalArgumentException("인증번호 입력 가능 횟수를 초과했습니다. 다시 발송해 주세요.");
            }
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다. 다시 확인해 주세요.");
        }

        // 인증 성공: 인증코드 캐시에서 제거 (1회용 소비)
        emailCodeMap.remove(trimmedEmail);

        // 가입 완료 시 검증할 수 있는 일회성 토큰 발급 (30분 유효)
        String verificationToken = UUID.randomUUID().toString();
        verifiedTokenMap.put(verificationToken, new VerificationTokenInfo(trimmedEmail, System.currentTimeMillis() + TOKEN_EXPIRE_MILLIS));

        return verificationToken;
    }

    @Override
    @Transactional
    public UserResponseDTO signup(SignupRequestDTO requestDTO) {
        // 1. 필수 입력값 검증
        if (requestDTO == null) {
            throw new IllegalArgumentException("회원가입 정보가 전달되지 않았습니다.");
        }
        if (CommonUtil.isEmpty(requestDTO.getUserName())) {
            throw new IllegalArgumentException("이름을 입력해 주세요.");
        }
        if (requestDTO.getUserName().trim().length() > USER_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("이름은 4자 이하로 입력해 주세요.");
        }
        if (CommonUtil.isEmpty(requestDTO.getUserNickname())) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        if (requestDTO.getUserNickname().trim().length() < NICKNAME_MIN_LENGTH
                || requestDTO.getUserNickname().trim().length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하로 입력해 주세요.");
        }
        if (CommonUtil.isEmpty(requestDTO.getUserEmail())) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        if (CommonUtil.isEmpty(requestDTO.getUserPw())) {
            throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
        }
        if (!PASSWORD_PATTERN.matcher(requestDTO.getUserPw()).matches()) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문 대문자, 소문자, 숫자, 특수문자를 각각 포함해야 합니다.");
        }
        if (!requestDTO.getUserPw().equals(requestDTO.getConfirmPw())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        String email = normalizeEmail(requestDTO.getUserEmail());
        String nickname = requestDTO.getUserNickname().trim();

        // 2. 이메일 인증 토큰 검증
        String token = requestDTO.getVerificationToken();
        if (CommonUtil.isEmpty(token)) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해 주세요.");
        }

        VerificationTokenInfo tokenInfo = verifiedTokenMap.get(token);
        if (tokenInfo == null || tokenInfo.isExpired()) {
            verifiedTokenMap.remove(token);
            throw new IllegalArgumentException("이메일 인증이 유효하지 않거나 만료되었습니다. 다시 인증해 주세요.");
        }
        if (!tokenInfo.email.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("인증받은 이메일 주소와 가입하려는 이메일 주소가 일치하지 않습니다.");
        }

        // 3. 중복 검사 (DB 제약조건 2차 방어)
        if (userDAO.countByEmail(email) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userDAO.countByNickname(nickname) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 4. 생년월일 파싱
        Date birthDate = null;
        if (!CommonUtil.isEmpty(requestDTO.getUserBirthDate())) {
            try {
                String birthDateText = requestDTO.getUserBirthDate().trim();
                LocalDate parsedBirthDate = LocalDate.parse(birthDateText);
                LocalDate latestAllowedBirthDate = LocalDate.now().minusYears(ADULT_AGE);
                if (parsedBirthDate.isBefore(MIN_BIRTH_DATE) || parsedBirthDate.isAfter(latestAllowedBirthDate)) {
                    throw new IllegalArgumentException("생년월일은 1900년 이후이며 만 19세 이상이어야 합니다.");
                }
                birthDate = Date.from(parsedBirthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("생년월일은 YYYY-MM-DD 형식의 실제 날짜로 입력해 주세요.");
            }
        }

        // 5. 비밀번호 BCrypt 해싱 단방향 암호화
        String encodedPassword = passwordEncoder.encode(requestDTO.getUserPw());

        // 6. 회원 DTO 구성 및 DB 등록
        UserDTO userDTO = UserDTO.builder()
                .userName(requestDTO.getUserName().trim())
                .userNickname(nickname)
                .userEmail(email)
                .userPw(encodedPassword)
                .userBirthDate(birthDate)
                .userType("USER")
                .jobCode(requestDTO.getJobCode())
                .occupationCode(requestDTO.getOccupationCode())
                .userIsDeleted(0)
                .build();

        userDAO.insertUser(userDTO);

        // 인증 토큰 1회용 소비 완료
        verifiedTokenMap.remove(token);

        log.info("새로운 회원이 성공적으로 가입되었습니다. (회원번호: {}, 이메일: {})", userDTO.getUserNum(), email);

        // 비밀번호 및 해시 제외된 응답 DTO 반환
        return UserResponseDTO.from(userDTO);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        // 1. 파라미터 유효성 검사
        if (requestDTO == null || CommonUtil.isEmpty(requestDTO.getUserEmail()) || CommonUtil.isEmpty(requestDTO.getUserPw())) {
            throw new IllegalArgumentException("이메일과 비밀번호를 모두 입력해 주세요.");
        }

        String email = normalizeEmail(requestDTO.getUserEmail());

        // 2. 이메일로 회원 조회
        UserDTO user = userDAO.selectByEmail(email);
        if (user == null) {
            // 이메일 존재 여부가 노출되지 않도록 인증 실패 메시지를 통일한다.
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 3. 탈퇴 계정 여부 확인
        if (user.getUserIsDeleted() != null && user.getUserIsDeleted() == 1) {
            // 탈퇴 여부로 이메일 존재 여부가 노출되지 않도록 일반 인증 실패 메시지를 사용한다.
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 4. BCrypt 비밀번호 일치 검증
        if (!passwordEncoder.matches(requestDTO.getUserPw(), user.getUserPw())) {
            // 이메일 존재 여부가 노출되지 않도록 인증 실패 메시지를 통일한다.
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 5. JWT Access Token 발급
        String accessToken = jwtProvider.createToken(user);
        long expiresIn = jwtProvider.getExpirationTime();

        log.info("회원 로그인 성공 (회원번호: {}, 이메일: {})", user.getUserNum(), user.getUserEmail());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(UserResponseDTO.from(user))
                .build();
    }

    @Override
    public UserResponseDTO getUserProfile(Long userNum) {
        if (userNum == null) {
            throw new IllegalArgumentException("회원 식별 번호가 누락되었습니다.");
        }
        UserDTO user = userDAO.selectByUserNum(userNum);
        if (user == null || (user.getUserIsDeleted() != null && user.getUserIsDeleted() == 1)) {
            throw new IllegalArgumentException("존재하지 않거나 탈퇴한 회원입니다.");
        }
        return UserResponseDTO.from(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateMyProfile(Long userNum, MyPageUpdateRequestDTO requestDTO) {
        if (userNum == null) {
            throw new IllegalArgumentException("회원 식별 번호가 누락되었습니다.");
        }
        if (requestDTO == null) {
            throw new IllegalArgumentException("수정할 정보가 없습니다.");
        }

        UserDTO user = userDAO.selectByUserNum(userNum);
        if (user == null || (user.getUserIsDeleted() != null && user.getUserIsDeleted() == 1)) {
            throw new IllegalArgumentException("존재하지 않거나 탈퇴한 회원입니다.");
        }

        // Mapper의 동적 UPDATE에 전달할 값만 별도 DTO에 담는다.
        UserDTO updateUser = UserDTO.builder().userNum(userNum).build();
        boolean hasChange = false;
        boolean passwordChanged = false;

        if (requestDTO.getUserNickname() != null) {
            String nickname = requestDTO.getUserNickname().trim();
            if (nickname.isEmpty()) {
                throw new IllegalArgumentException("닉네임을 입력해 주세요.");
            }
            if (nickname.length() < NICKNAME_MIN_LENGTH || nickname.length() > NICKNAME_MAX_LENGTH) {
                throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하로 입력해 주세요.");
            }
            if (!nickname.equals(user.getUserNickname())) {
                if (userDAO.countByNickname(nickname) > 0) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
                updateUser.setUserNickname(nickname);
                hasChange = true;
            }
        }

        // 비밀번호 관련 필드 중 하나라도 전달되면 세 필드를 모두 검증한다.
        boolean hasPasswordInput = requestDTO.getCurrentPassword() != null
                || requestDTO.getNewPassword() != null
                || requestDTO.getNewPasswordConfirm() != null;
        if (hasPasswordInput) {
            if (CommonUtil.isEmpty(requestDTO.getCurrentPassword())
                    || CommonUtil.isEmpty(requestDTO.getNewPassword())
                    || CommonUtil.isEmpty(requestDTO.getNewPasswordConfirm())) {
                throw new IllegalArgumentException("비밀번호를 변경하려면 현재 비밀번호와 새 비밀번호를 모두 입력해 주세요.");
            }
            // DB에는 BCrypt 해시만 저장하므로 현재 비밀번호는 matches로 비교한다.
            if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), user.getUserPw())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            if (!PASSWORD_PATTERN.matcher(requestDTO.getNewPassword()).matches()) {
                throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문 대소문자, 숫자, 특수문자를 각각 포함해야 합니다.");
            }
            if (!requestDTO.getNewPassword().equals(requestDTO.getNewPasswordConfirm())) {
                throw new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            }
            if (passwordEncoder.matches(requestDTO.getNewPassword(), user.getUserPw())) {
                throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 다르게 설정해 주세요.");
            }

            // 새 비밀번호는 평문으로 저장하지 않고 BCrypt 해시로 변경한다.
            updateUser.setUserPw(passwordEncoder.encode(requestDTO.getNewPassword()));
            hasChange = true;
            passwordChanged = true;
        }

        if (!hasChange) {
            return UserResponseDTO.from(user);
        }
        if (userDAO.updateMyProfile(updateUser) != 1) {
            throw new IllegalStateException("회원 정보 수정에 실패했습니다.");
        }

        // DB 저장 뒤 토큰 버전을 증가시켜 비밀번호 변경 전 JWT를 영구적으로 무효화한다.
        if (passwordChanged) {
            if (userDAO.incrementTokenVersion(userNum) != 1) {
                throw new IllegalStateException("기존 로그인 정보를 만료하지 못했습니다.");
            }
        }

        return getUserProfile(userNum);
    }

    @Override
    @Transactional
    public void withdrawMyAccount(Long userNum, WithdrawalRequestDTO requestDTO) {
        if (userNum == null || requestDTO == null || CommonUtil.isEmpty(requestDTO.getCurrentPassword())) {
            throw new IllegalArgumentException("회원 탈퇴를 위해 현재 비밀번호를 입력해 주세요.");
        }

        UserDTO user = userDAO.selectByUserNum(userNum);
        if (user == null || (user.getUserIsDeleted() != null && user.getUserIsDeleted() == 1)) {
            throw new IllegalArgumentException("이미 탈퇴했거나 존재하지 않는 회원입니다.");
        }

        // 탈퇴 요청은 JWT뿐 아니라 현재 비밀번호까지 일치해야 처리한다.
        if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), user.getUserPw())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 연관 데이터를 삭제하지 않고 로그인과 재가입을 막는 탈퇴 플래그만 변경한다.
        if (userDAO.withdrawMyAccount(userNum) != 1) {
            throw new IllegalStateException("회원 탈퇴 처리에 실패했습니다.");
        }

    }
}
