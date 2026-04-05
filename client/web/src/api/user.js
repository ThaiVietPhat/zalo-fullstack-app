import api from './axios';

export const getAllUsers = () =>
  api.get('/api/v1/user');

export const getMyProfile = () =>
  api.get('/api/v1/user/me');

export const updateMyProfile = (data) =>
  api.put('/api/v1/user/me', data);

export const changePassword = (data) =>
  api.patch('/api/v1/user/me/password', data);

export const uploadAvatar = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/api/v1/user/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const searchUsers = (keyword) =>
  api.get('/api/v1/user/search', { params: { keyword } });
