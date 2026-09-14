import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

function Signup() {
    const [formData, setFormData] = useState({
        name: "",
        email: "",
        password: "",
        confirmPassword: "",
    });

    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        if (formData.password !== formData.confirmPassword) {
            alert("비밀번호가 일치하지 않습니다.");
            return;
        }

        // 테스트용 회원가입 처리 (추후 백엔드 회원가입 API 연동)
        alert(`회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.`);
        navigate("/login");
    };

    return (
        <div className="row justify-content-center my-5">
            <div className="col-12 col-md-7 col-lg-6">
                <div className="card shadow-sm border-0">
                    <div className="card-body p-4 p-md-5">
                        <h3 className="card-title fw-bold text-center mb-4">회원가입</h3>

                        <form onSubmit={handleSubmit}>
                            <div className="mb-3">
                                <label htmlFor="signupName" className="form-label">
                                    이름
                                </label>
                                <input
                                    type="text"
                                    className="form-control"
                                    id="signupName"
                                    name="name"
                                    placeholder="홍길동"
                                    value={formData.name}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="signupEmail" className="form-label">
                                    이메일
                                </label>
                                <input
                                    type="email"
                                    className="form-control"
                                    id="signupEmail"
                                    name="email"
                                    placeholder="name@example.com"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="signupPassword" className="form-label">
                                    비밀번호
                                </label>
                                <input
                                    type="password"
                                    className="form-control"
                                    id="signupPassword"
                                    name="password"
                                    placeholder="비밀번호 입력"
                                    value={formData.password}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="confirmPassword" className="form-label">
                                    비밀번호 확인
                                </label>
                                <input
                                    type="password"
                                    className="form-control"
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    placeholder="비밀번호 재입력"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <button type="submit" className="btn btn-primary w-100 py-2 mt-3 fw-bold">
                                가입하기
                            </button>
                        </form>

                        <div className="text-center mt-4 text-muted small">
                            이미 계정이 있으신가요?{" "}
                            <Link to="/login" className="text-primary text-decoration-none fw-bold">
                                로그인하기
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Signup;
