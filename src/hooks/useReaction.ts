import { useMutation, useQueryClient } from "@tanstack/react-query";
import { reactToMessage, reactToGroupMessage, removeReaction, removeGroupReaction } from "@/api/reaction";

export const useReactToMessage = (chatId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: string; emoji: string }) =>
      reactToMessage(messageId, emoji),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", chatId] });
    },
  });
};

export const useReactToGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: string; emoji: string }) =>
      reactToGroupMessage(messageId, emoji),
    onMutate: async ({ messageId, emoji }) => {
      // Optimistic update: thả emoji ngay lập tức không chờ server
      queryClient.setQueryData(['group-messages', groupId, 0], (old: any[] | undefined) => {
        if (!old) return old;
        return old.map((m: any) => {
          if (m.id !== messageId) return m;
          const existing = m.reactions || [];
          const idx = existing.findIndex((r: any) => r.emoji === emoji);
          const reactions = idx >= 0 ? existing.filter((_: any, i: number) => i !== idx) : [...existing, { emoji, userId: 'me' }];
          return { ...m, reactions };
        });
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group-messages', groupId, 0] });
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ['group-messages', groupId, 0] });
    },
  });
};
