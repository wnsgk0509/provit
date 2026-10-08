import { Link } from "react-router-dom";
import "./Footer.css";

function Footer() {
  return (
    <footer className="provit-footer">
      <div className="container provit-footer-content">
        <div className="provit-footer-intro">
          <Link className="provit-footer-brand" to="/">Provit</Link>
          <p className="provit-footer-description">AI와 함께 준비하는 더 나은 취업 여정</p>
        </div>

        <address className="provit-footer-contact">
          <h2 className="provit-footer-heading">연락처</h2>
          <span>충청남도 천안시 동남구 대흥동 134</span>
          <a
            href="https://www.google.com/search?q=%ED%9C%B4%EB%A8%BC%EA%B5%90%EC%9C%A1%EC%84%BC%ED%84%B0"
            target="_blank"
            rel="noreferrer"
          >
            041-561-1122
          </a>
          <a href="mailto:provit.korea@gmail.com">provit.korea@gmail.com</a>
        </address>

        <nav className="provit-footer-policy" aria-label="정책 메뉴">
          <h2 className="provit-footer-heading">정책</h2>
          <Link to="/privacy-policy">개인정보 처리 방침</Link>
          <Link to="/terms-of-service">사이트 이용 약관</Link>
          <Link to="/cookie-policy">쿠키 정책</Link>
        </nav>
      </div>
      <p className="container provit-footer-copyright mb-0">© {new Date().getFullYear()} Provit. All rights reserved.</p>
    </footer>
  );
}

export default Footer;
