import { useEffect, useRef } from 'react';
import { BrowserRouter, Routes, Route, useNavigate, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Home from './pages/home/Home';
import JobList from './pages/jobs/JobList';
import Fortune from './pages/fortune/Fortune';
import Mbti from './pages/mbti/Mbti';
import MbtiResult from './pages/mbti/MbtiResult';
import Interview from './pages/interview/Interview';
import InterviewAccessGate from './pages/interview/components/InterviewAccessGate';
import CommunityList from './pages/community/CommunityList';
import CommunityWrite from './pages/community/CommunityWrite';
import CommunityDetail from './pages/community/CommunityDetail';
import CommunityEdit from './pages/community/CommunityEdit';
import MyPage from './pages/mypage/MyPage';
import DocumentWrite from './pages/documentWrite/DocumentWrite';
import DocumentRead from './pages/documentRead/DocumentRead';
import DocumentEdit from './pages/documentEdit/DocumentEdit';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import BootstrapTemplate from './pages/bootstrap';
import AdminReportList from './pages/admin/AdminReportList';
import AdminUserList from './pages/admin/AdminUserList';

function RequireAuth({ children, alertMessage }) {
  const { isLoading, isLoggedIn, isSessionExpired, clearSessionExpired } = useAuth();
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  useEffect(() => {
    if (isLoading || isLoggedIn || hasRedirected.current) return;

    hasRedirected.current = true;
    const message = isSessionExpired
      ? '로그인 시간이 만료되었습니다. 다시 로그인해 주세요.'
      : alertMessage;
    if (message) {
      window.alert(message);
    }
    if (isSessionExpired) clearSessionExpired();
    navigate('/login', { replace: true });
  }, [alertMessage, clearSessionExpired, isLoading, isLoggedIn, isSessionExpired, navigate]);

  if (isLoading || !isLoggedIn) return null;

  return children;
}

function RequireAdmin({ children, alertMessage }) {
  const { isLoading, isLoggedIn, user } = useAuth();
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  useEffect(() => {
    if (isLoading || hasRedirected.current) return;

    if (!isLoggedIn || user?.userType !== 'ADMIN') {
      hasRedirected.current = true;
      if (alertMessage) {
        window.alert(alertMessage);
      }
      navigate('/home', { replace: true });
    }
  }, [alertMessage, isLoading, isLoggedIn, user, navigate]);

  if (isLoading || !isLoggedIn || user?.userType !== 'ADMIN') return null;

  return children;
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main className="container my-4">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/home" element={<Home />} />
            <Route path="/jobs" element={<JobList />} />
            <Route path="/fortune" element={<Fortune />} />
            <Route path="/mbti" element={<Mbti />} />
            <Route path="/mbti/result/:mbtiType" element={<MbtiResult />} />
            <Route
              path="/interview"
              element={(
                <InterviewAccessGate>
                  <RequireAuth alertMessage="로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.">
                    <Interview />
                  </RequireAuth>
                </InterviewAccessGate>
              )}
            />
            <Route path="/community" element={<CommunityList />} />
            <Route path="/community/write" element={<CommunityWrite />} />
            <Route path="/community/:postNum" element={<CommunityDetail />} />
            <Route path="/community/edit/:postNum" element={<CommunityEdit />} />
            <Route path="/study" element={<Navigate to="/community?tab=study" replace />} />
            <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
            <Route path="/documents/write" element={<RequireAuth><DocumentWrite /></RequireAuth>} />
            <Route path="/documents/:documentType/:documentId/edit" element={<RequireAuth><DocumentEdit /></RequireAuth>} />
            <Route path="/documents/:documentType/:documentId" element={<RequireAuth><DocumentRead /></RequireAuth>} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            {/* 관리자 라우트 */}
            <Route path="/admin/reports" element={<RequireAdmin alertMessage="관리자만 접근 가능합니다."><AdminReportList /></RequireAdmin>} />
            <Route path="/admin/users" element={<RequireAdmin alertMessage="관리자만 접근 가능합니다."><AdminUserList /></RequireAdmin>} />
            {/* 팀원 참고용 부트스트랩 템플릿 화면 */}
            <Route path="/bootstrap" element={<BootstrapTemplate />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
