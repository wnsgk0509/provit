import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import client from "../../api/client";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [emailError, setEmailError] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrorMessage("");
        setEmailError("");
        setPasswordError("");

        if (!email.trim() || !password) {
            if (!email.trim()) setEmailError("이메일을 입력해 주세요.");
            if (!password) setPasswordError("비밀번호를 입력해 주세요.");
            return;
        }

        setLoading(true);

        try {
            const response = await client.post("/auth/login", {
                userEmail: email.trim(),
                userPw: password,
            });

            if (response.data && response.data.data) {
                const { accessToken, user } = response.data.data;
                login(accessToken, user);
                navigate("/");
            } else {
                setErrorMessage("로그인 처리 중 문제가 발생했습니다.");
            }
        } catch (error) {
            const resData = error.response?.data;
            // ApiResponse의 data에 담긴 필드별 로그인 실패 사유를 우선 사용한다.
            const message = typeof resData?.data === "string"
                ? resData.data
                : resData?.responseCode?.message;

            if (message === "이메일이 일치하지 않습니다.") {
                setEmailError(message);
            } else if (message === "비밀번호가 일치하지 않습니다.") {
                setPasswordError(message);
            } else if (message) {
                setErrorMessage(message);
            } else {
                setErrorMessage("서버와 통신할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            }
        } finally {
            setLoading(false);
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
                                <p className="text-muted small">AI 모의 면접 및 맞춤형 취업 지원 플랫폼</p>
                            </div>

                            <h4 className="fw-bold text-center mb-4">로그인</h4>

                            {errorMessage && (
                                <div className="alert alert-danger py-2 px-3 small rounded-3 mb-3 text-break" role="alert">
                                    {errorMessage}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} noValidate>
                                <div className="mb-3">
                                    <label htmlFor="loginEmail" className="form-label fw-semibold small text-secondary">
                                        이메일 계정
                                    </label>
                                    <input
                                        type="email"
                                        className={`form-control form-control-lg fs-6 py-2 ${emailError ? "is-invalid" : ""}`}
                                        id="loginEmail"
                                        placeholder="name@example.com"
                                        value={email}
                                        onChange={(e) => {
                                            setEmail(e.target.value);
                                            setEmailError("");
                                        }}
                                        disabled={loading}
                                        required
                                        autoComplete="email"
                                    />
                                    {emailError && <div className="invalid-feedback d-block">{emailError}</div>}
                                </div>

                                <div className="mb-4">
                                    <label htmlFor="loginPassword" className="form-label fw-semibold small text-secondary">
                                        비밀번호
                                    </label>
                                    <input
                                        type="password"
                                        className={`form-control form-control-lg fs-6 py-2 ${passwordError ? "is-invalid" : ""}`}
                                        id="loginPassword"
                                        placeholder="비밀번호를 입력하세요"
                                        value={password}
                                        onChange={(e) => {
                                            setPassword(e.target.value);
                                            setPasswordError("");
                                        }}
                                        disabled={loading}
                                        required
                                        autoComplete="current-password"
                                    />
                                    {passwordError && <div className="invalid-feedback d-block">{passwordError}</div>}
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-primary btn-lg w-100 py-2 fs-6 fw-bold shadow-sm d-flex align-items-center justify-content-center"
                                    disabled={loading}
                                >
                                    {loading ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                            로그인 중...
                                        </>
                                    ) : (
                                        "로그인"
                                    )}
                                </button>
                            </form>

                            <div className="text-center mt-4 text-muted small">
                                아직 계정이 없으신가요?{" "}
                                <Link to="/signup" className="text-primary text-decoration-none fw-bold ms-1">
                                    회원가입하기
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Login;
