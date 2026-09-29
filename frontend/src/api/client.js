import axios from 'axios';

/**
 * 공통 Axios 인스턴스
 * Vite 개발 서버의 Proxy(/api -> localhost:8080)를 활용하여 CORS 문제 없이 통신합니다.
 */
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
});

// 응답 인터셉터 (공통 에러 처리 및 401 토큰 만료 처리)
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data?.data?.maintenance === true) {
      window.dispatchEvent(new Event('interview:maintenance'));
    }
    if (error.response && error.response.status === 401) {
      // 만료되거나 위조된 토큰 제거
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

export default client;
