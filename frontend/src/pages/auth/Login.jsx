import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!email || !password) {
            alert("이메일과 비밀번호를 입력해 주세요.");
            return;
        }

        // 테스트용 간편 로그인 처리 (추후 백엔드 로그인 API 연동)
        login({ name: email.split("@")[0] || "사용자", email });
        alert("로그인되었습니다!");
        navigate("/");
    };

    return (
        <div className="row justify-content-center my-5">
            <div className="col-12 col-md-6 col-lg-5">
                <div className="card shadow-sm border-0">
                    <div className="card-body p-4 p-md-5">
                        <h3 className="card-title fw-bold text-center mb-4">로그인</h3>

                        <form onSubmit={handleSubmit}>
                            <div className="mb-3">
                                <label htmlFor="loginEmail" className="form-label">
                                    이메일 (아이디)
                                </label>
                                <input
                                    type="email"
                                    className="form-control"
                                    id="loginEmail"
                                    placeholder="name@example.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="loginPassword" className="form-label">
                                    비밀번호
                                </label>
                                <input
                                    type="password"
                                    className="form-control"
                                    id="loginPassword"
                                    placeholder="비밀번호를 입력하세요"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                />
                            </div>

                            <button type="submit" className="btn btn-primary w-100 py-2 mt-3 fw-bold">
                                로그인
                            </button>
                        </form>

                        <div className="text-center mt-4 text-muted small">
                            계정이 없으신가요?{" "}
                            <Link to="/signup" className="text-primary text-decoration-none fw-bold">
                                회원가입하기
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Login;
