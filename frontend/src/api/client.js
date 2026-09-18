import axios from 'axios';

/**
 * 공통 Axios 인스턴스
 * Vite 개발 서버의 Proxy(/api -> localhost:8080)를 활용하여 CORS 문제 없이 통신합니다.
 */
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터 (JWT 토큰 자동 첨부)
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터 (공통 에러 처리 및 401 토큰 만료 처리)
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // 만료되거나 위조된 토큰 제거
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

export default client;
