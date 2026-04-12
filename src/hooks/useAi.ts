import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { chatWithAi, getAiHistory, clearAiHistory } from "@/api/ai";

export const useAiHistory = (page = 0) => {
  return useQuery({
    queryKey: ["ai-history", page],
    queryFn: () => getAiHistory(page),
  });
};

export const useChatWithAi = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (message: string) => chatWithAi(message),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai-history"] });
    },
  });
};

export const useClearAiHistory = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: clearAiHistory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai-history"] });
    },
  });
};
