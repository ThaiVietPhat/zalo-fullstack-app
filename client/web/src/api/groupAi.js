import api from './axios';

/**
 * Feature 1 — Smart Reply
 * Lấy 3 gợi ý trả lời dựa trên context tin nhắn gần nhất trong nhóm.
 */
export const getSmartReplies = (groupId) =>
  api.post(`/api/v1/group/${groupId}/ai/smart-reply`);

/**
 * Feature 2 — Message Summarization
 * Tóm tắt tin nhắn từ thời điểm `since` (ISO string) đến hiện tại.
 */
export const summarizeGroup = (groupId, since) =>
  api.post(`/api/v1/group/${groupId}/ai/summarize`, { since });
