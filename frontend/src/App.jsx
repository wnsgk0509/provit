import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Home from './pages/home/Home';
import JobList from './pages/jobs/JobList';
import Fortune from './pages/fortune/Fortune';
import Interview from './pages/interview/Interview';
import Study from './pages/study/Study';
import CommunityList from './pages/community/CommunityList';
import MyPage from './pages/mypage/MyPage';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import BootstrapTemplate from './pages/bootstrap';

function RequireAuth({ children }) {
  const { isLoading, isLoggedIn } = useAuth();

  if (isLoading) return null;

  return isLoggedIn ? children : <Navigate to="/login" replace />;
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
            <Route path="/interview" element={<Interview />} />
            <Route path="/community" element={<CommunityList />} />
            {/* <Route path="/study" element={<Study />} /> 기존 개별 스터디 라우트는 커뮤니티로 통합 */}
            <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
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
