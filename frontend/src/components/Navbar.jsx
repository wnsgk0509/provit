import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Navbar() {
    const location = useLocation();
    const { isLoggedIn, user, logout } = useAuth();

    const isActive = (path) => (location.pathname === path ? "active fw-bold text-primary" : "text-light");

    return (
        <nav className="navbar navbar-dark bg-dark shadow-sm py-2">
            <div className="container d-flex align-items-center justify-content-between flex-nowrap">
                {/* 좌측: 로고 및 바로 펼쳐지는 메인 네비게이션 메뉴 */}
                <div className="d-flex align-items-center gap-4">
                    {/* 브랜드 로고 */}
                    <Link className="navbar-brand fw-bold text-primary m-0" to="/">
                        Provit
                    </Link>

                    {/* GNB 메뉴 목록 (가로 일렬 노출) */}
                    <ul className="navbar-nav d-flex flex-row align-items-center gap-3 m-0 list-unstyled">
                        <li className="nav-item">
                            <Link className={`nav-link py-1 ${isActive("/")}`} to="/">
                                홈
                            </Link>
                        </li>
                        <li className="nav-item">
                            <Link className={`nav-link py-1 ${isActive("/jobs")}`} to="/jobs">
                                채용 공고
                            </Link>
                        </li>
                        <li className="nav-item">
                            <Link className={`nav-link py-1 ${isActive("/fortune")}`} to="/fortune">
                                오늘의 운세
                            </Link>
                        </li>
                        <li className="nav-item">
                            <Link className={`nav-link py-1 ${isActive("/interview")}`} to="/interview">
                                AI 모의 면접실
                            </Link>
                        </li>
                        <li className="nav-item">
                            <Link className={`nav-link py-1 ${isActive("/community")}`} to="/community">
                                커뮤니티
                            </Link>
                        </li>

                        {/* 로그인되어 있을 때만 마이페이지 노출 */}
                        {isLoggedIn && (
                            <li className="nav-item">
                                <Link className={`nav-link py-1 ${isActive("/mypage")}`} to="/mypage">
                                    마이페이지
                                </Link>
                            </li>
                        )}
                    </ul>
                </div>

                {/* 우측: 인증 영역 (로그인 / 회원가입 or 사용자 정보 / 로그아웃) */}
                <div className="d-flex align-items-center gap-2">
                    {isLoggedIn ? (
                        <>
                            <span className="text-light me-2 small">
                                <strong className="text-info">{user?.name || "사용자"}</strong>님 환영합니다
                            </span>
                            <button type="button" className="btn btn-outline-danger btn-sm" onClick={logout}>
                                로그아웃
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="btn btn-outline-light btn-sm">
                                로그인
                            </Link>
                            <Link to="/signup" className="btn btn-primary btn-sm">
                                회원가입
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
