import axiosInstance from './axios';

/**
 * Lưu lịch sử cuộc gọi sau khi kết thúc.
 * Chỉ caller gọi endpoint này.
 */
export const saveCallSession = (data) =>
  axiosInstance.post('/calls', data);

/**
 * Lấy lịch sử cuộc gọi của một chat.
 */
export const getCallHistory = (chatId) =>
  axiosInstance.get(`/calls/${chatId}`);
