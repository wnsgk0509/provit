import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useModal } from "../../context/ModalContext";

const PASSWORD_PATTERN = /^(?=\S{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).*$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function PasswordReset() {
    const navigate = useNavigate();
    const { logout } = useAuth();
    const { showAlert } = useModal();
    const timerRef = useRef(null);

    const [email, setEmail] = useState("");
    const [authCode, setAuthCode] = useState("");
    const [verificationToken, setVerificationToken] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [timer, setTimer] = useState(0);
    const [isCodeSent, setIsCodeSent] = useState(false);
    const [isVerified, setIsVerified] = useState(false);
    const [emailMsg, setEmailMsg] = useState("");
    const [emailError, setEmailError] = useState("");
    const [loadingAction, setLoadingAction] = useState("");
    const [alertMsg, setAlertMsg] = useState({ type: "", text: "" });
    const [fieldErrors, setFieldErrors] = useState({});

    useEffect(() => {
        if (!isCodeSent || isVerified || timer <= 0) return undefined;

        timerRef.current = setInterval(() => {
            setTimer((previous) => Math.max(previous - 1, 0));
        }, 1000);

        return () => clearInterval(timerRef.current);
    }, [isCodeSent, isVerified, timer]);

    useEffect(() => () => clearInterval(timerRef.current), []);

    const formatTime = (seconds) => {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        return `${minutes}:${String(remainingSeconds).padStart(2, "0")}`;
    };

    const getErrorMessage = (error, fallback) => {
        const response = error.response?.data;
        return response?.responseCode?.message || response?.data || fallback;
    };

    const setFieldError = (field, message) => {
        setFieldErrors((previous) => ({ ...previous, [field]: message }));
    };

    const clearFieldError = (field) => {
        setAlertMsg({ type: "", text: "" });
        setFieldErrors((previous) => {
            if (!previous[field]) return previous;
            const next = { ...previous };
            delete next[field];
            return next;
        });
    };

    const resetVerificationState = () => {
        clearInterval(timerRef.current);
        setAuthCode("");
        setVerificationToken("");
        setTimer(0);
        setIsCodeSent(false);
        setIsVerified(false);
        setEmailMsg("");
        setEmailError("");
        setFieldErrors({});
        setNewPassword("");
        setConfirmPassword("");
    };

    const handleEmailChange = (event) => {
        setEmail(event.target.value);
        resetVerificationState();
        setAlertMsg({ type: "", text: "" });
    };

    const handleSendCode = async () => {
        const normalizedEmail = email.trim();
        if (!EMAIL_PATTERN.test(normalizedEmail)) {
            setEmailError("올바른 이메일 주소를 입력해 주세요.");
            setAlertMsg({ type: "danger", text: "올바른 이메일 주소를 입력해 주세요." });
            return;
        }

        setLoadingAction("send");
        setEmailError("");
        setEmailMsg("");
        setAlertMsg({ type: "", text: "" });
        try {
            const response = await client.post(
                "/auth/password-reset/send-code",
                { email: normalizedEmail },
                { timeout: 30000 }
            );
            const expiresAt = response.data?.data?.expiresAt;
            if (typeof expiresAt !== "number") {
                throw new Error("인증번호 만료 시각을 받지 못했습니다.");
            }

            setIsCodeSent(true);
            setIsVerified(false);
            setVerificationToken("");
            setAuthCode("");
            setTimer(Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000)));
            setEmailMsg("인증번호를 발송했습니다. 이메일을 확인해 주세요.");
            setAlertMsg({ type: "success", text: "인증번호를 이메일로 발송했습니다." });
        } catch (error) {
            const message = getErrorMessage(error, "인증번호 발송에 실패했습니다.");
            setEmailError(message);
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingAction("");
        }
    };

    const handleVerifyCode = async () => {
        setEmailError("");
        clearFieldError("authCode");
        if (!/^\d{6}$/.test(authCode.trim())) {
            setFieldError("authCode", "인증번호 6자리를 입력해 주세요.");
            setAlertMsg({ type: "danger", text: "인증번호 6자리를 입력해 주세요." });
            return;
        }
        if (timer <= 0) {
            setFieldError("authCode", "인증번호가 만료되었습니다. 다시 발송해 주세요.");
            setAlertMsg({ type: "danger", text: "인증번호가 만료되었습니다. 다시 발송해 주세요." });
            return;
        }

        setLoadingAction("verify");
        setAlertMsg({ type: "", text: "" });
        try {
            const response = await client.post("/auth/password-reset/verify-code", {
                email: email.trim(),
                code: authCode.trim(),
            });
            const token = response.data?.data?.verificationToken;
            if (!token) {
                throw new Error("비밀번호 재설정 토큰을 받지 못했습니다.");
            }

            clearInterval(timerRef.current);
            setVerificationToken(token);
            setIsVerified(true);
            setEmailMsg("이메일 인증이 완료되었습니다.");
            setAlertMsg({ type: "success", text: "이메일 인증이 완료되었습니다. 새 비밀번호를 입력해 주세요." });
        } catch (error) {
            const message = getErrorMessage(error, "인증번호가 일치하지 않습니다.");
            setFieldError("authCode", message);
            setAlertMsg({ type: "danger", text: message });
        } finally {
            setLoadingAction("");
        }
    };

    const handleResetPassword = async (event) => {
        event.preventDefault();
        setEmailError("");
        setFieldErrors({});
        if (!isVerified || !verificationToken) {
            setEmailError("이메일 인증을 먼저 완료해 주세요.");
            setAlertMsg({ type: "danger", text: "이메일 인증을 먼저 완료해 주세요." });
            return;
        }
        if (!PASSWORD_PATTERN.test(newPassword)) {
            setFieldError("newPassword", "비밀번호는 8자 이상이며 영문 대소문자, 숫자, 특수문자를 각각 포함해야 합니다.");
            setAlertMsg({ type: "danger", text: "비밀번호는 8자 이상이며 영문 대소문자, 숫자, 특수문자를 각각 포함해야 합니다." });
            return;
        }
        if (newPassword !== confirmPassword) {
            setFieldError("confirmPassword", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            setAlertMsg({ type: "danger", text: "새 비밀번호와 비밀번호 확인이 일치하지 않습니다." });
            return;
        }

        setLoadingAction("reset");
        setAlertMsg({ type: "", text: "" });
        try {
            await client.post("/auth/password-reset", {
                verificationToken,
                newPassword,
                confirmPassword,
            });
            await logout();
            await showAlert("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.", { title: "비밀번호 변경 완료", type: "success" });
            navigate("/login", { replace: true });
        } catch (error) {
            setAlertMsg({ type: "danger", text: getErrorMessage(error, "비밀번호 변경에 실패했습니다.") });
        } finally {
            setLoadingAction("");
        }
    };

    return (
        <div className="container py-4 py-md-5">
            <div className="row justify-content-center">
                <div className="col-12 col-sm-10 col-md-8 col-lg-5">
                    <div className="card shadow-sm border-0 rounded-4">
                        <div className="card-body p-4 p-md-5">
                            <div className="text-center mb-4">
                                <h2 className="fw-bold text-primary mb-1">Provit</h2>
                                <p className="text-muted small mb-0">비밀번호를 재설정합니다.</p>
                            </div>

                            {alertMsg.text && alertMsg.type !== "success" && !emailError && Object.keys(fieldErrors).length === 0 && (
                                <div className={`alert alert-${alertMsg.type} py-2 px-3 small rounded-3 mb-3 text-break`} role="alert">
                                    {alertMsg.text}
                                </div>
                            )}

                            <form onSubmit={handleResetPassword} noValidate>
                                <div className="mb-3">
                                    <label htmlFor="resetEmail" className="form-label fw-semibold small text-secondary">이메일 계정</label>
                                    <div className="input-group">
                                        <input
                                            id="resetEmail"
                                            type="email"
                                            className="form-control form-control-lg fs-6 py-2"
                                            placeholder="name@example.com"
                                            value={email}
                                            onChange={handleEmailChange}
                                            disabled={isVerified || Boolean(loadingAction)}
                                            autoComplete="email"
                                            required
                                        />
                                        <button
                                            type="button"
                                            className="btn btn-outline-primary px-3"
                                            onClick={handleSendCode}
                                            disabled={isVerified || Boolean(loadingAction)}
                                        >
                                            {loadingAction === "send" ? "발송 중" : isCodeSent ? "재발송" : "인증번호 발송"}
                                        </button>
                                    </div>
                                    {emailMsg && (
                                        <div className={`small mt-1 ${isVerified ? "text-success" : "text-primary"}`}>
                                            {emailMsg}
                                        </div>
                                    )}
                                    {emailError && <div className="small mt-1 text-danger">{emailError}</div>}
                                </div>

                                {isCodeSent && !isVerified && (
                                    <div className="mb-3 p-3 bg-light rounded-3 border">
                                        <div className="d-flex justify-content-between align-items-center mb-2">
                                            <label htmlFor="resetCode" className="small fw-semibold text-secondary">인증번호 6자리</label>
                                            <span className={`small fw-bold ${timer < 60 ? "text-danger" : "text-primary"}`}>남은 시간: {formatTime(timer)}</span>
                                        </div>
                                        <div className="input-group">
                                            <input
                                                id="resetCode"
                                                type="text"
                                                inputMode="numeric"
                                                maxLength="6"
                                                className="form-control form-control-lg fs-6 py-2 text-center"
                                                placeholder="123456"
                                                value={authCode}
                                                onChange={(event) => {
                                                    setAuthCode(event.target.value.replace(/\D/g, ""));
                                                    clearFieldError("authCode");
                                                }}
                                                disabled={Boolean(loadingAction)}
                                            />
                                            <button
                                                type="button"
                                                className="btn btn-primary px-3"
                                                onClick={handleVerifyCode}
                                                disabled={timer === 0 || Boolean(loadingAction)}
                                            >
                                                {loadingAction === "verify" ? "확인 중" : "확인"}
                                            </button>
                                        </div>
                                        {fieldErrors.authCode && <div className="small mt-1 text-danger">{fieldErrors.authCode}</div>}
                                    </div>
                                )}

                                {isVerified && (
                                    <>
                                        <div className="mb-3">
                                            <label htmlFor="newPassword" className="form-label fw-semibold small text-secondary">새 비밀번호</label>
                                            <input
                                                id="newPassword"
                                                type="password"
                                                className="form-control form-control-lg fs-6 py-2"
                                                placeholder="8자 이상 · 영문 대/소문자·숫자·특수문자 포함"
                                                value={newPassword}
                                                onChange={(event) => {
                                                    setNewPassword(event.target.value);
                                                    clearFieldError("newPassword");
                                                }}
                                                disabled={loadingAction === "reset"}
                                                autoComplete="new-password"
                                                required
                                            />
                                            {fieldErrors.newPassword && <div className="small mt-1 text-danger">{fieldErrors.newPassword}</div>}
                                        </div>
                                        <div className="mb-4">
                                            <label htmlFor="confirmNewPassword" className="form-label fw-semibold small text-secondary">새 비밀번호 확인</label>
                                            <input
                                                id="confirmNewPassword"
                                                type="password"
                                                className="form-control form-control-lg fs-6 py-2"
                                                placeholder="새 비밀번호를 다시 입력하세요"
                                                value={confirmPassword}
                                                onChange={(event) => {
                                                    setConfirmPassword(event.target.value);
                                                    clearFieldError("confirmPassword");
                                                }}
                                                disabled={loadingAction === "reset"}
                                                autoComplete="new-password"
                                                required
                                            />
                                            {fieldErrors.confirmPassword && <div className="small mt-1 text-danger">{fieldErrors.confirmPassword}</div>}
                                        </div>
                                        <button
                                            type="submit"
                                            className="btn btn-primary btn-lg w-100 py-2 fs-6 fw-bold"
                                            disabled={loadingAction === "reset"}
                                        >
                                            {loadingAction === "reset" ? "변경 중..." : "비밀번호 변경"}
                                        </button>
                                    </>
                                )}
                            </form>

                            <div className="text-center mt-4 text-muted small">
                                <Link to="/login" className="text-primary text-decoration-none fw-bold">로그인으로 돌아가기</Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default PasswordReset;
