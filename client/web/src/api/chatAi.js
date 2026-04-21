import api from './axios';

/**
 * Feature 1 — Smart Reply cho chat 1-1
 * Lấy 3 gợi ý trả lời dựa trên context tin nhắn gần nhất.
 */
export const getChatSmartReplies = (chatId) =>
  api.post(`/api/v1/chat/${chatId}/ai/smart-reply`);

/**
 * Feature 2 — Message Summarization cho chat 1-1
 * Tóm tắt tin nhắn từ thời điểm `since` (ISO string) đến hiện tại.
 */
export const summarizeChat = (chatId, since) =>
  api.post(`/api/v1/chat/${chatId}/ai/summarize`, { since });
