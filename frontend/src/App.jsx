import { useEffect, useRef } from 'react';
import { BrowserRouter, Routes, Route, useNavigate, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Home from './pages/home/Home';
import JobList from './pages/jobs/JobList';
import Fortune from './pages/fortune/Fortune';
import Interview from './pages/interview/Interview';
import CommunityList from './pages/community/CommunityList';
import CommunityWrite from './pages/community/CommunityWrite';
import CommunityDetail from './pages/community/CommunityDetail';
import CommunityEdit from './pages/community/CommunityEdit';
import MyPage from './pages/mypage/MyPage';
import DocumentWrite from './pages/documentWrite/DocumentWrite';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import BootstrapTemplate from './pages/bootstrap';

function RequireAuth({ children, alertMessage }) {
  const { isLoading, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const hasRedirected = useRef(false);

  useEffect(() => {
    if (isLoading || isLoggedIn || hasRedirected.current) return;

    hasRedirected.current = true;
    if (alertMessage) {
      window.alert(alertMessage);
    }
    navigate('/login', { replace: true });
  }, [alertMessage, isLoading, isLoggedIn, navigate]);

  if (isLoading || !isLoggedIn) return null;

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
            <Route
              path="/interview"
              element={(
                <RequireAuth alertMessage="로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.">
                  <Interview />
                </RequireAuth>
              )}
            />
            <Route path="/community" element={<CommunityList />} />
            <Route path="/community/write" element={<CommunityWrite />} />
            <Route path="/community/:postNum" element={<CommunityDetail />} />
            <Route path="/community/edit/:postNum" element={<CommunityEdit />} />
            <Route path="/study" element={<Navigate to="/community?tab=study" replace />} />
            <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
            <Route path="/documents/write" element={<RequireAuth><DocumentWrite /></RequireAuth>} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            {/* 팀원 참고용 부트스트랩 템플릿 화면 */}
            <Route path="/bootstrap" element={<BootstrapTemplate />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
