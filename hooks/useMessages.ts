// hooks/useMessages.ts — đồng bộ với MessageDto.java (state/deleted thay vì seen/recalled)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getMessagesByChatId,
  sendMessage,
  markMessagesAsSeen,
  recallMessage,
  deleteMessageForMe,
  uploadMediaMessage,
  MessageDto,
} from "@/api/message";

/** Lấy tin nhắn của 1 chat (phân trang) */
export const useMessages = (chatId: string | null, page = 0, size = 30) => {
  return useQuery<MessageDto[]>({
    queryKey: ["messages", chatId, page],
    queryFn: () => getMessagesByChatId(chatId!, page, size),
    enabled: !!chatId,
    staleTime: 10_000,
  });
};

/** Gửi tin nhắn văn bản (REST fallback) */
export const useSendMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: sendMessage,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["messages", variables.chatId] });
      queryClient.invalidateQueries({ queryKey: ["chats"] });
    },
  });
};

/** Upload media (ảnh, video, file) */
export const useUploadMedia = (chatId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => uploadMediaMessage(chatId, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", chatId] });
      queryClient.invalidateQueries({ queryKey: ["chats"] });
    },
  });
};

/** Đánh dấu đã xem */
export const useMarkSeen = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (chatId: string) => markMessagesAsSeen(chatId),
    onSuccess: (_, chatId) => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
      queryClient.invalidateQueries({ queryKey: ["messages", chatId] });
    },
  });
};

/** Thu hồi tin nhắn */
export const useRecallMessage = (chatId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => recallMessage(messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", chatId] });
    },
  });
};

/** Xóa tin nhắn phía mình */
export const useDeleteMessage = (chatId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => deleteMessageForMe(messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", chatId] });
    },
  });
};
