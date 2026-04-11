import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getMyGroups, createGroup, sendGroupMessage, uploadGroupMedia } from "@/api/group";
import { MessageDto } from "@/api/message";

export const useMyGroups = () => {
  return useQuery({
    queryKey: ["groups"],
    queryFn: getMyGroups,
  });
};

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createGroup,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useSendGroupMessage = (groupId: string) => {
  return useMutation({
    mutationFn: (payload: Partial<MessageDto>) => 
      sendGroupMessage(groupId, payload),
  });
};

export const useUploadGroupMedia = (groupId: string) => {
  return useMutation({
    mutationFn: (formData: FormData) => 
      uploadGroupMedia(groupId, formData),
  });
};
