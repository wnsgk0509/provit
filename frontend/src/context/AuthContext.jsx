import { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import client, { refreshAccessToken } from '../api/client';

const AuthContext = createContext();
const AUTH_SESSION_KEY = 'provit:auth-session';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSessionExpired, setSessionExpired] = useState(false);
  const lastRefreshAtRef = useRef(0);

  useEffect(() => {
    const initAuth = async () => {
      try {
        await refreshAccessToken();
        lastRefreshAtRef.current = Date.now();
        const res = await client.get('/auth/me');
        if (res.data?.data) {
          localStorage.setItem(AUTH_SESSION_KEY, 'true');
          setUser(res.data.data);
          setSessionExpired(false);
        }
      } catch {
        setSessionExpired(localStorage.getItem(AUTH_SESSION_KEY) === 'true');
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();

    const handleUnauthorized = () => {
      setSessionExpired(localStorage.getItem(AUTH_SESSION_KEY) === 'true');
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  useEffect(() => {
    if (!user) return undefined;

    const renewOnActivity = () => {
      const twentyMinutes = 20 * 60 * 1000;
      if (Date.now() - lastRefreshAtRef.current < twentyMinutes) return;

      refreshAccessToken()
        .then(() => { lastRefreshAtRef.current = Date.now(); })
        .catch(() => { window.dispatchEvent(new Event('auth:unauthorized')); });
    };

    const activityEvents = ['pointerdown', 'keydown', 'scroll', 'touchstart'];
    activityEvents.forEach((eventName) => window.addEventListener(eventName, renewOnActivity, { passive: true }));
    return () => activityEvents.forEach((eventName) => window.removeEventListener(eventName, renewOnActivity));
  }, [user]);

  const login = (userData) => {
    lastRefreshAtRef.current = Date.now();
    localStorage.setItem(AUTH_SESSION_KEY, 'true');
    setSessionExpired(false);
    setUser(userData);
  };

  const logout = async () => {
    try {
      await client.post('/auth/logout');
    } catch {
      // 만료되었거나 유효하지 않은 쿠키여도 화면의 로그인 상태는 초기화합니다.
    } finally {
      lastRefreshAtRef.current = 0;
      localStorage.removeItem(AUTH_SESSION_KEY);
      setSessionExpired(false);
      setUser(null);
    }
  };

  const updateUser = useCallback((userData) => setUser(userData), []);
  const clearSessionExpired = () => {
    localStorage.removeItem(AUTH_SESSION_KEY);
    setSessionExpired(false);
  };
  const isLoggedIn = !!user;

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, isLoading, isSessionExpired, login, logout, updateUser, clearSessionExpired }}>
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
