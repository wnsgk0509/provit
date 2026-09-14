import { createContext, useContext, useState } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  // 로그인 여부 상태 (기본값: false)
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  // 로그인 사용자 정보 (기본값: null)
  const [user, setUser] = useState(null);

  // 테스트용 간편 로그인 (클릭 시 로그인 상태로 전환)
  const login = (userData = { name: '홍길동' }) => {
    setIsLoggedIn(true);
    setUser(userData);
  };

  // 로그아웃
  const logout = () => {
    setIsLoggedIn(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
