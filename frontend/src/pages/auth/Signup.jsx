import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../../api/client";

const PASSWORD_PATTERN = /^(?=\S{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).*$/;
// 백엔드 회원 정보 검증과 동일한 입력 길이 기준이다.
const USER_NAME_MAX_LENGTH = 4;
const NICKNAME_MIN_LENGTH = 2;
const NICKNAME_MAX_LENGTH = 20;

function Signup() {
    const navigate = useNavigate();

    // 입력 폼 상태
    const [formData, setFormData] = useState({
        userName: "",
        userNickname: "",
        userEmail: "",
        userBirthDate: "",
        userPw: "",
        confirmPw: "",
    });

    // 이메일 인증 상태
    const [authCode, setAuthCode] = useState("");
    const [isEmailSent, setIsEmailSent] = useState(false);
    const [isEmailVerified, setIsEmailVerified] = useState(false);
    const [verificationToken, setVerificationToken] = useState("");
    const [timer, setTimer] = useState(0); // 초 단위 (300초 = 5분)
    const [isEmailChecked, setIsEmailChecked] = useState(false);
    const [isEmailAvailable, setIsEmailAvailable] = useState(false);
    const [emailMsg, setEmailMsg] = useState("");

    // 닉네임 중복확인 상태
    const [isNicknameChecked, setIsNicknameChecked] = useState(false);
    const [nicknameMsg, setNicknameMsg] = useState("");
    const [isNicknameAvailable, setIsNicknameAvailable] = useState(false);
    const [isNicknameMaxLengthExceeded, setIsNicknameMaxLengthExceeded] = useState(false);
    const [isNameMaxLengthExceeded, setIsNameMaxLengthExceeded] = useState(false);

    // 입력 중에도 서버 규칙과 같은 길이 오류를 즉시 안내한다.
    const isNameTooLong = isNameMaxLengthExceeded;
    const isNicknameLengthInvalid = isNicknameMaxLengthExceeded || (Boolean(formData.userNickname.trim())
        && (formData.userNickname.trim().length < NICKNAME_MIN_LENGTH
            || formData.userNickname.trim().length > NICKNAME_MAX_LENGTH));

    // 로딩 및 에러/성공 메시지
    const [loadingEmailSend, setLoadingEmailSend] = useState(false);
    const [loadingEmailCheck, setLoadingEmailCheck] = useState(false);
    const [loadingEmailVerify, setLoadingEmailVerify] = useState(false);
    const [loadingSubmit, setLoadingSubmit] = useState(false);
    const [alertMsg, setAlertMsg] = useState({ type: "", text: "" });

    const timerRef = useRef(null);
    const today = new Date();
    const adultBirthDate = new Date(
        today.getFullYear() - 19,
        today.getMonth(),
        today.getDate()
    );
    const maxAdultBirthDate = `${adultBirthDate.getFullYear()}-${String(adultBirthDate.getMonth() + 1).padStart(2, "0")}-${String(adultBirthDate.getDate()).padStart(2, "0")}`;
    const minBirthDate = "1900-01-01";
    const passwordRequirements = [
        { label: "8자 이상", met: formData.userPw.length >= 8 },
        { label: "영문 대문자 포함", met: /[A-Z]/.test(formData.userPw) },
        { label: "영문 소문자 포함", met: /[a-z]/.test(formData.userPw) },
        { label: "숫자 포함", met: /\d/.test(formData.userPw) },
        { label: "특수문자 포함", met: /[^A-Za-z0-9\s]/.test(formData.userPw) },
    ];

    // 타이머 관리
    useEffect(() => {
        if (timer > 0) {
            timerRef.current = setInterval(() => {
                setTimer((prev) => prev - 1);
            }, 1000);
        } else if (timer === 0 && isEmailSent && !isEmailVerified) {
            clearInterval(timerRef.current);
        }
        return () => clearInterval(timerRef.current);
    }, [timer, isEmailSent, isEmailVerified]);

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        const isNicknameTooLong = name === "userNickname" && value.length > NICKNAME_MAX_LENGTH;
        const isNameTooLong = name === "userName" && value.length > USER_NAME_MAX_LENGTH;
        // 이름·닉네임은 최대 길이까지만 저장하고, 초과 입력은 하단 안내로 알린다.
        const nextValue = isNicknameTooLong
            ? value.slice(0, NICKNAME_MAX_LENGTH)
            : isNameTooLong ? value.slice(0, USER_NAME_MAX_LENGTH) : value;

        if (name === "userBirthDate" && value && (value < minBirthDate || value > maxAdultBirthDate)) {
            setAlertMsg({ type: "danger", text: "생년월일은 1900년 이후의 만 19세 이상 날짜만 입력할 수 있습니다." });
            return;
        }

        setFormData((prev) => ({ ...prev, [name]: nextValue }));

        if (name === "userNickname") {
            setIsNicknameChecked(false);
            setIsNicknameMaxLengthExceeded(isNicknameTooLong);
            setNicknameMsg("");
        }
        if (name === "userName") {
            setIsNameMaxLengthExceeded(isNameTooLong);
        }
        if (name === "userEmail") {
            setIsEmailChecked(false);
            setIsEmailAvailable(false);
            setEmailMsg("");
            setIsEmailSent(false);
            setIsEmailVerified(false);
            setVerificationToken("");
            setAuthCode("");
            setTimer(0);
        }
    };

    // 이메일 중복 확인 후에만 인증번호를 발송할 수 있습니다.
    const handleCheckEmail = async () => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const email = formData.userEmail.trim();
        if (!email || !emailRegex.test(email)) {
            setAlertMsg({ type: "danger", text: "올바른 이메일 주소를 입력해 주세요." });
            return;
        }

        setLoadingEmailCheck(true);
        setAlertMsg({ type: "", text: "" });
        try {
            const res = await client.get("/auth/check-email", { params: { email } });
            const available = res.data?.data?.available === true;
            setIsEmailChecked(true);
            setIsEmailAvailable(available);
            setEmailMsg(available ? "사용 가능한 이메일입니다. 인증코드를 발송해 주세요." : "이미 가입된 이메일입니다.");
        } catch (error) {
            const message = error.response?.data?.data || "이메일 중복 확인 중 오류가 발생했습니다.";
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingEmailCheck(false);
        }
    };

    // 1. 닉네임 중복 확인
    const handleCheckNickname = async () => {
        const nickname = formData.userNickname.trim();
        if (!nickname) {
            setAlertMsg({ type: "danger", text: "닉네임을 입력해 주세요." });
            return;
        }
        if (nickname.length < NICKNAME_MIN_LENGTH || nickname.length > NICKNAME_MAX_LENGTH) {
            setIsNicknameChecked(false);
            setIsNicknameAvailable(false);
            setNicknameMsg("닉네임은 2자 이상 20자 이하로 입력해 주세요.");
            return;
        }

        try {
            const res = await client.get("/auth/check-nickname", {
                params: { nickname },
            });
            const available = res.data?.data?.available;
            setIsNicknameChecked(true);
            setIsNicknameAvailable(available);
            if (available) {
                setNicknameMsg("사용 가능한 닉네임입니다.");
            } else {
                setNicknameMsg("이미 사용 중인 닉네임입니다.");
            }
        } catch {
            setAlertMsg({ type: "danger", text: "닉네임 확인 중 오류가 발생했습니다." });
        }
    };

    // 2. 이메일 인증번호 발송
    const handleSendVerificationCode = async () => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!formData.userEmail.trim() || !emailRegex.test(formData.userEmail.trim())) {
            setAlertMsg({ type: "danger", text: "올바른 이메일 주소를 입력해 주세요." });
            return;
        }
        if (!isEmailChecked || !isEmailAvailable) {
            setAlertMsg({ type: "danger", text: "이메일 중복 확인 후 인증코드를 발송해 주세요." });
            return;
        }

        setLoadingEmailSend(true);
        setAlertMsg({ type: "", text: "" });

        try {
            await client.post(
                "/auth/send-code",
                { email: formData.userEmail.trim() },
                // SMTP 연결·전송은 일반 API보다 오래 걸릴 수 있습니다.
                { timeout: 30000 }
            );
            setIsEmailSent(true);
            setTimer(300); // 5분 (300초)
            setAlertMsg({ type: "success", text: "인증번호가 발송되었습니다. 메일함을 확인해 주세요." });
        } catch (error) {
            const resData = error.response?.data;
            const message = resData?.responseCode?.message || resData?.data || "인증번호 발송에 실패했습니다.";
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingEmailSend(false);
        }
    };

    // 3. 이메일 인증번호 확인
    const handleVerifyCode = async () => {
        if (!authCode.trim()) {
            setAlertMsg({ type: "danger", text: "인증번호 6자리를 입력해 주세요." });
            return;
        }
        if (timer === 0) {
            setAlertMsg({ type: "danger", text: "인증번호 유효시간이 만료되었습니다. 다시 발송해 주세요." });
            return;
        }

        setLoadingEmailVerify(true);
        setAlertMsg({ type: "", text: "" });

        try {
            const res = await client.post("/auth/verify-code", {
                email: formData.userEmail.trim(),
                code: authCode.trim(),
            });

            const token = res.data?.data?.verificationToken;
            if (token) {
                setVerificationToken(token);
                setIsEmailVerified(true);
                clearInterval(timerRef.current);
                setAlertMsg({ type: "success", text: "이메일 인증이 완료되었습니다!" });
            }
        } catch (error) {
            const resData = error.response?.data;
            const message = resData?.responseCode?.message || resData?.data || "인증번호가 일치하지 않습니다.";
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingEmailVerify(false);
        }
    };

    // 4. 최종 회원가입 제출
    const handleSubmit = async (e) => {
        e.preventDefault();
        setAlertMsg({ type: "", text: "" });

        // 유효성 종합 검증
        if (!formData.userName.trim()) {
            setAlertMsg({ type: "danger", text: "이름을 입력해 주세요." });
            return;
        }
        if (isNameMaxLengthExceeded || formData.userName.trim().length > USER_NAME_MAX_LENGTH) {
            setAlertMsg({ type: "danger", text: "이름은 4자 이하로 입력해 주세요." });
            return;
        }
        if (!formData.userNickname.trim()) {
            setAlertMsg({ type: "danger", text: "닉네임을 입력해 주세요." });
            return;
        }
        if (isNicknameMaxLengthExceeded) {
            setAlertMsg({ type: "danger", text: "닉네임은 2자 이상 20자 이하로 입력해 주세요." });
            return;
        }
        if (formData.userNickname.trim().length < NICKNAME_MIN_LENGTH
            || formData.userNickname.trim().length > NICKNAME_MAX_LENGTH) {
            setAlertMsg({ type: "danger", text: "닉네임은 2자 이상 20자 이하로 입력해 주세요." });
            return;
        }
        if (!isNicknameChecked || !isNicknameAvailable) {
            setAlertMsg({ type: "danger", text: "닉네임 중복 확인을 진행해 주세요." });
            return;
        }
        if (!isEmailVerified || !verificationToken) {
            setAlertMsg({ type: "danger", text: "이메일 인증을 완료해 주세요." });
            return;
        }
        if (!formData.userPw) {
            setAlertMsg({ type: "danger", text: "비밀번호를 입력해 주세요." });
            return;
        }
        if (!PASSWORD_PATTERN.test(formData.userPw)) {
            setAlertMsg({ type: "danger", text: "비밀번호는 8자 이상이며 영문 대문자, 소문자, 숫자, 특수문자를 각각 포함해야 합니다." });
            return;
        }
        if (formData.userPw !== formData.confirmPw) {
            setAlertMsg({ type: "danger", text: "비밀번호가 일치하지 않습니다." });
            return;
        }

        setLoadingSubmit(true);

        try {
            const payload = {
                userName: formData.userName.trim(),
                userNickname: formData.userNickname.trim(),
                userEmail: formData.userEmail.trim(),
                userBirthDate: formData.userBirthDate || null,
                userPw: formData.userPw,
                confirmPw: formData.confirmPw,
                verificationToken: verificationToken,
            };

            const res = await client.post("/auth/signup", payload);

            if (res.status === 201 || res.data?.responseCode?.code === 301) {
                alert("회원가입이 성공적으로 완료되었습니다! 로그인 페이지로 이동합니다.");
                navigate("/login");
            }
        } catch (error) {
            const resData = error.response?.data;
            const message = resData?.responseCode?.message || resData?.data || "회원가입 중 오류가 발생했습니다.";
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingSubmit(false);
        }
    };

    return (
        <div className="container py-4 py-md-5">
            <div className="row justify-content-center">
                <div className="col-12 col-sm-11 col-md-9 col-lg-7 col-xl-6">
                    <div className="card shadow-sm border-0 rounded-4">
                        <div className="card-body p-4 p-md-5">
                            <div className="text-center mb-4">
                                <h2 className="fw-bold text-primary mb-1">Provit</h2>
                                <p className="text-muted small">새로운 도전을 위한 맞춤형 취업 플랫폼</p>
                            </div>

                            <h4 className="fw-bold text-center mb-4">회원가입</h4>

                            {alertMsg.text && (
                                <div className={`alert alert-${alertMsg.type} py-2 px-3 small rounded-3 mb-4 text-break`} role="alert">
                                    {alertMsg.text}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} noValidate>
                                {/* 1. 이름 */}
                                <div className="mb-3">
                                    <label htmlFor="signupName" className="form-label fw-semibold small text-secondary">
                                        이름 <span className="text-danger">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        className="form-control form-control-lg fs-6 py-2"
                                        id="signupName"
                                        name="userName"
                                        placeholder="홍길동"
                                        value={formData.userName}
                                        onChange={handleChange}
                                        required
                                    />
                                    {isNameTooLong && <div className="small mt-1 text-danger">이름은 4자 이하로 입력해 주세요.</div>}
                                </div>

                                {/* 2. 닉네임 + 중복확인 버튼 (반응형 배치) */}
                                <div className="mb-3">
                                    <label htmlFor="signupNickname" className="form-label fw-semibold small text-secondary">
                                        닉네임 <span className="text-danger">*</span>
                                    </label>
                                    <div className="input-group">
                                        <input
                                            type="text"
                                            className="form-control form-control-lg fs-6 py-2"
                                            id="signupNickname"
                                            name="userNickname"
                                            placeholder="활동용 닉네임"
                                        value={formData.userNickname}
                                        onChange={handleChange}
                                        required
                                        />
                                        <button
                                            type="button"
                                            className="btn btn-outline-secondary px-3 fw-medium"
                                            onClick={handleCheckNickname}
                                        >
                                            중복확인
                                        </button>
                                    </div>
                                    {isNicknameLengthInvalid ? (
                                        <div className="small mt-1 text-danger">닉네임은 2자 이상 20자 이하로 입력해 주세요.</div>
                                    ) : nicknameMsg && (
                                        <div className={`small mt-1 ${isNicknameAvailable ? "text-success" : "text-danger"}`}>
                                            {nicknameMsg}
                                        </div>
                                    )}
                                </div>

                                {/* 3. 생년월일 (선택) */}
                                <div className="mb-3">
                                    <label htmlFor="signupBirth" className="form-label fw-semibold small text-secondary">
                                        생년월일 <span className="text-muted fw-normal">(선택)</span>
                                    </label>
                                    <input
                                        type="date"
                                        className="form-control form-control-lg fs-6 py-2"
                                        id="signupBirth"
                                        name="userBirthDate"
                                        value={formData.userBirthDate}
                                        onChange={handleChange}
                                        min={minBirthDate}
                                        max={maxAdultBirthDate}
                                    />
                                </div>

                                {/* 4. 이메일 + 인증번호 발송 버튼 (반응형 배치) */}
                                <div className="mb-3">
                                    <label htmlFor="signupEmail" className="form-label fw-semibold small text-secondary">
                                        이메일 (아이디) <span className="text-danger">*</span>
                                    </label>
                                    <div className="input-group">
                                        <input
                                            type="email"
                                            className="form-control form-control-lg fs-6 py-2"
                                            id="signupEmail"
                                            name="userEmail"
                                            placeholder="name@example.com"
                                            value={formData.userEmail}
                                            onChange={handleChange}
                                            readOnly={isEmailVerified}
                                            required
                                        />
                                        <button
                                            type="button"
                                            className="btn btn-outline-secondary px-3 fw-medium"
                                            onClick={handleCheckEmail}
                                            disabled={loadingEmailCheck || isEmailVerified}
                                        >
                                            {loadingEmailCheck ? (
                                                <span className="spinner-border spinner-border-sm" role="status"></span>
                                            ) : (
                                                "중복확인"
                                            )}
                                        </button>
                                        <button
                                            type="button"
                                            className={`btn ${isEmailVerified ? "btn-success" : "btn-outline-primary"} px-3 fw-medium`}
                                            onClick={handleSendVerificationCode}
                                            disabled={loadingEmailSend || isEmailVerified || !isEmailChecked || !isEmailAvailable}
                                        >
                                            {loadingEmailSend ? (
                                                <span className="spinner-border spinner-border-sm" role="status"></span>
                                            ) : isEmailVerified ? (
                                                "인증완료"
                                            ) : isEmailSent ? (
                                                "재발송"
                                            ) : (
                                                "인증코드 발송"
                                            )}
                                        </button>
                                    </div>
                                    {emailMsg && (
                                        <div className={`small mt-1 ${isEmailAvailable ? "text-success" : "text-danger"}`}>
                                            {emailMsg}
                                        </div>
                                    )}
                                </div>

                                {/* 5. 인증번호 입력창 및 타이머 (발송 완료 시 노출) */}
                                {isEmailSent && !isEmailVerified && (
                                    <div className="mb-3 p-3 bg-light rounded-3 border">
                                        <div className="d-flex justify-content-between align-items-center mb-2">
                                            <span className="small fw-semibold text-secondary">인증번호 6자리</span>
                                            <span className={`small fw-bold ${timer < 60 ? "text-danger" : "text-primary"}`}>
                                                남은 시간: {formatTime(timer)}
                                            </span>
                                        </div>
                                        <div className="input-group">
                                            <input
                                                type="text"
                                                maxLength="6"
                                                className="form-control form-control-lg fs-6 py-2 text-center letter-spacing-2"
                                                placeholder="123456"
                                                value={authCode}
                                                onChange={(e) => setAuthCode(e.target.value)}
                                            />
                                            <button
                                                type="button"
                                                className="btn btn-primary px-3 fw-medium"
                                                onClick={handleVerifyCode}
                                                disabled={loadingEmailVerify || timer === 0}
                                            >
                                                {loadingEmailVerify ? (
                                                    <span className="spinner-border spinner-border-sm" role="status"></span>
                                                ) : (
                                                    "확인"
                                                )}
                                            </button>
                                        </div>
                                    </div>
                                )}

                                {/* 6. 비밀번호 */}
                                <div className="mb-3">
                                    <label htmlFor="signupPassword" className="form-label fw-semibold small text-secondary">
                                        비밀번호 <span className="text-danger">*</span>
                                    </label>
                                    <input
                                        type="password"
                                        className="form-control form-control-lg fs-6 py-2"
                                        id="signupPassword"
                                        name="userPw"
                                        placeholder="8자 이상 · 영문 대/소문자·숫자·특수문자 포함"
                                        value={formData.userPw}
                                        onChange={handleChange}
                                        required
                                        autoComplete="new-password"
                                    />
                                    {formData.userPw && (
                                        <div className="small mt-2" aria-live="polite">
                                            {passwordRequirements.map((requirement) => (
                                                <div key={requirement.label} className={requirement.met ? "text-success" : "text-danger"}>
                                                    {requirement.met ? "✓" : "•"} {requirement.label}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* 7. 비밀번호 확인 */}
                                <div className="mb-4">
                                    <label htmlFor="confirmPassword" className="form-label fw-semibold small text-secondary">
                                        비밀번호 확인 <span className="text-danger">*</span>
                                    </label>
                                    <input
                                        type="password"
                                        className="form-control form-control-lg fs-6 py-2"
                                        id="confirmPassword"
                                        name="confirmPw"
                                        placeholder="비밀번호 재입력"
                                        value={formData.confirmPw}
                                        onChange={handleChange}
                                        required
                                        autoComplete="new-password"
                                    />
                                    {formData.confirmPw && formData.userPw !== formData.confirmPw && (
                                        <div className="small text-danger mt-1">비밀번호가 일치하지 않습니다.</div>
                                    )}
                                    {formData.confirmPw && formData.userPw === formData.confirmPw && PASSWORD_PATTERN.test(formData.userPw) && (
                                        <div className="small text-success mt-1">비밀번호가 일치합니다.</div>
                                    )}
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-primary btn-lg w-100 py-2 fs-6 fw-bold shadow-sm d-flex align-items-center justify-content-center"
                                    disabled={loadingSubmit}
                                >
                                    {loadingSubmit ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                            가입 처리 중...
                                        </>
                                    ) : (
                                        "가입하기"
                                    )}
                                </button>
                            </form>

                            <div className="text-center mt-4 text-muted small">
                                이미 계정이 있으신가요?{" "}
                                <Link to="/login" className="text-primary text-decoration-none fw-bold ms-1">
                                    로그인하기
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Signup;
