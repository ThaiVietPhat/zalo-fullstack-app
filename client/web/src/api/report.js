import api from './axios';

export const createReport = (userId, data) =>
  api.post(`/api/v1/report/${userId}`, data);

export const getReports = (status, page = 0, size = 20) =>
  api.get('/api/v1/admin/reports', { params: { status, page, size } });

export const resolveReport = (id, data) =>
  api.patch(`/api/v1/admin/reports/${id}/resolve`, data);
