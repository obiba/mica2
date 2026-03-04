import { defineBoot } from '#q-app/wrappers';
import axios from 'axios';

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: typeof axios;
    $api: typeof api;
  }
}

export const api = axios.create({
  baseURL: process.env.API,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

function getContextPath(): string {
  const path = window.location.pathname;
  const idx = path.indexOf('/mica-ui');
  if (idx >= 0) {
    return path.substring(0, idx) || '/';
  }
  return '/';
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      try {
        await api.get('/auth/session/_current');
      } catch {
        const contextPath = getContextPath();
        window.location.assign(contextPath);
      }
    }
    return Promise.reject(error);
  },
);

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
});
