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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", groupId] });
    },
  });
};
