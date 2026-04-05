// hooks/useChat.ts — đồng bộ với ChatDto.java (flat structure)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getAllChats, getChatById, startOrGetChat, ChatDto } from "@/api/chat";
import { getAllUsers, searchUsers, UserDto } from "@/api/user";

// ─── Chat hooks ───────────────────────────────────────────────────────────────

/** Lấy danh sách tất cả cuộc hội thoại */
export const useChats = () => {
  return useQuery<ChatDto[]>({
    queryKey: ["chats"],
    queryFn: getAllChats,
    staleTime: 30_000,
  });
};

/** Lấy chi tiết 1 cuộc hội thoại */
export const useChatById = (chatId: string | null) => {
  return useQuery<ChatDto>({
    queryKey: ["chat", chatId],
    queryFn: () => getChatById(chatId!),
    enabled: !!chatId,
  });
};

/** Bắt đầu / lấy cuộc hội thoại với user khác */
export const useStartChat = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (otherUserId: string) => startOrGetChat(otherUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
    },
  });
};

// ─── User/Contact hooks ───────────────────────────────────────────────────────

/** Lấy danh sách tất cả user (danh bạ), trừ bản thân */
export const useUsers = () => {
  return useQuery<UserDto[]>({
    queryKey: ["users"],
    queryFn: getAllUsers,
    staleTime: 60_000,
  });
};

/** Tìm kiếm user theo keyword */
export const useSearchUsers = (keyword: string) => {
  return useQuery<UserDto[]>({
    queryKey: ["users-search", keyword],
    queryFn: () => searchUsers(keyword),
    enabled: keyword.trim().length > 0,
  });
};
