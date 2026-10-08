import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
});

let refreshRequest = null;

export function refreshAccessToken() {
  if (!refreshRequest) {
    refreshRequest = client.post('/auth/refresh', null, { skipAuthRefresh: true })
      .finally(() => {
        refreshRequest = null;
      });
  }

  return refreshRequest;
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isUnauthorized = error.response?.status === 401;
    const isLoginRequest = originalRequest?.url === '/auth/login';

    if (error.response?.data?.data?.maintenance === true) {
      window.dispatchEvent(new Event('interview:maintenance'));
    }

    if (isUnauthorized && originalRequest && !isLoginRequest
      && !originalRequest.skipAuthRefresh && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        await refreshAccessToken();
        return client(originalRequest);
      } catch {
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    } else if (isUnauthorized && !isLoginRequest && !originalRequest?.skipAuthRefresh) {
      window.dispatchEvent(new Event('auth:unauthorized'));
    }

    return Promise.reject(error);
  },
);

export default client;
