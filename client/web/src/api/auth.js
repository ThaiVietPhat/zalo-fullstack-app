import api from './axios';

export const register = (data) =>
  api.post('/api/v1/auth/register', data);

export const login = (data) =>
  api.post('/api/v1/auth/login', data);

export const refreshToken = (refreshToken) =>
  api.post('/api/v1/auth/refresh', { refreshToken });

export const logout = () =>
  api.post('/api/v1/auth/logout');
