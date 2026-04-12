import { fetchAPI } from "@/lib/fetch";

export interface AiMessageDto {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdDate: string;
}

export const chatWithAi = async (message: string): Promise<AiMessageDto> => {
  return fetchAPI("/ai/chat", {
    method: "POST",
    body: JSON.stringify({ message }),
  });
};

export const getAiHistory = async (page = 0, size = 30): Promise<{ content: AiMessageDto[] }> => {
  return fetchAPI(`/ai/history?page=${page}&size=${size}`);
};

export const clearAiHistory = async (): Promise<void> => {
  await fetchAPI("/ai/history", {
    method: "DELETE",
  });
};
