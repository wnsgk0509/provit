import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Menu, X } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

const navItems = [
    { path: "/interview", label: "AI 모의면접" },
    { path: "/jobs", label: "채용공고" },
    { path: "/community", label: "커뮤니티" },
    { path: "/fortune", label: "오늘의 운세/MBTI" },
];

function Navbar() {
    const location = useLocation();
    const { isLoggedIn, user, logout } = useAuth();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isScrolled, setIsScrolled] = useState(false);

    const isActive = (path) => location.pathname === path;

    useEffect(() => {
        // 스크롤 위치에 따라 고정 Navbar의 투명도를 조절한다.
        const handleScroll = () => setIsScrolled(window.scrollY > 12);

        handleScroll();
        window.addEventListener("scroll", handleScroll, { passive: true });
        return () => window.removeEventListener("scroll", handleScroll);
    }, []);

    useEffect(() => {
        // 화면 이동 뒤 모바일 메뉴가 열린 채 남지 않도록 닫는다.
        setIsMenuOpen(false);
    }, [location.pathname]);

    const handleLogout = () => {
        logout();
        setIsMenuOpen(false);
    };

    return (
        <nav className={`provit-navbar ${isScrolled ? "is-scrolled" : ""}`}>
            <div className="container provit-navbar-container">
                <Link className="provit-navbar-brand" to="/" aria-label="Provit 홈으로 이동">
                    Provit
                </Link>

                <button
                    type="button"
                    className="provit-navbar-toggle"
                    onClick={() => setIsMenuOpen((open) => !open)}
                    aria-expanded={isMenuOpen}
                    aria-controls="provit-navbar-menu"
                    aria-label={isMenuOpen ? "메뉴 닫기" : "메뉴 열기"}
                >
                    {isMenuOpen ? <X size={23} /> : <Menu size={23} />}
                </button>

                <div id="provit-navbar-menu" className={`provit-navbar-menu ${isMenuOpen ? "is-open" : ""}`}>
                    <ul className="provit-navbar-links">
                        {navItems.map(({ path, label }) => (
                            <li key={path}>
                                <Link className={isActive(path) ? "is-active" : ""} to={path}>
                                    {label}
                                </Link>
                            </li>
                        ))}
                    </ul>

                    <div className="provit-navbar-auth">
                        {isLoggedIn ? (
                            <>
                                <Link className="provit-navbar-mypage" to="/mypage">
                                    {user?.userNickname || user?.userName || user?.name || "사용자"}님
                                </Link>
                                <button type="button" className="provit-navbar-logout" onClick={handleLogout}>
                                    로그아웃
                                </button>
                            </>
                        ) : (
                            <Link className="provit-navbar-login" to="/login">로그인</Link>
                        )}
                    </div>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
