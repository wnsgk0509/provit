import { createContext, useContext, useState, useEffect } from 'react';
import client from '../api/client';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        // 브라우저가 HttpOnly 인증 쿠키를 자동으로 전송합니다.
        const res = await client.get('/auth/me');
        if (res.data?.data) {
          setUser(res.data.data);
        }
      } catch {
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();

    const handleUnauthorized = () => setUser(null);
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = (userData) => setUser(userData);

  const logout = async () => {
    try {
      await client.post('/auth/logout');
    } catch {
      // 만료되었거나 유효하지 않은 쿠키여도 화면의 로그인 상태는 초기화합니다.
    } finally {
      setUser(null);
    }
  };

  const updateUser = (userData) => setUser(userData);
  const isLoggedIn = !!user;

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, isLoading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
