import api from './axios';

export const getAdminUsers = (page = 0, size = 20) =>
  api.get('/api/v1/admin/users', { params: { page, size } });

export const banUser = (userId) =>
  api.patch(`/api/v1/admin/users/${userId}/ban`);

export const unbanUser = (userId) =>
  api.patch(`/api/v1/admin/users/${userId}/unban`);

export const deleteUser = (userId) =>
  api.delete(`/api/v1/admin/users/${userId}`);

export const promoteUser = (userId) =>
  api.patch(`/api/v1/admin/users/${userId}/promote`);

export const demoteUser = (userId) =>
  api.patch(`/api/v1/admin/users/${userId}/demote`);

export const getAdminGroups = (page = 0, size = 20) =>
  api.get('/api/v1/admin/groups', { params: { page, size } });

export const deleteAdminGroup = (groupId) =>
  api.delete(`/api/v1/admin/groups/${groupId}`);

export const getStats = () =>
  api.get('/api/v1/admin/stats');
