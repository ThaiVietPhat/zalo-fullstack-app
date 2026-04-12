import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getMyGroups, createGroup, sendGroupMessage, uploadGroupMedia,
  getGroupById, getGroupMessages, recallGroupMessage, deleteGroupMessageForMe,
  updateGroup, uploadGroupAvatar, addGroupMembers, removeGroupMember,
  setGroupAdmin, leaveGroup, dissolveGroup,
} from "@/api/group";
import { MessageDto } from "@/api/message";

export const useMyGroups = () => {
  return useQuery({
    queryKey: ["groups"],
    queryFn: getMyGroups,
  });
};

export const useGroupById = (groupId: string | null) => {
  return useQuery({
    queryKey: ["group", groupId],
    queryFn: () => getGroupById(groupId!),
    enabled: !!groupId,
  });
};

export const useGroupMessages = (groupId: string | null, page = 0, size = 30) => {
  return useQuery({
    queryKey: ["group-messages", groupId, page],
    queryFn: () => getGroupMessages(groupId!, page, size),
    enabled: !!groupId,
    refetchInterval: 3000,       // polling mỗi 3 giây
    refetchIntervalInBackground: false,
    staleTime: 0,                // luôn xem là cũ để refetch
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
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: Partial<MessageDto>) => sendGroupMessage(groupId, payload),
    onSuccess: () => {
      // Reload tin nhắn ngay sau khi gửi
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useUploadGroupMedia = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => uploadGroupMedia(groupId, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useRecallGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => recallGroupMessage(groupId, messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
    },
  });
};

export const useDeleteGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => deleteGroupMessageForMe(groupId, messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
    },
  });
};

export const useUpdateGroup = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => updateGroup(groupId, { name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useUploadGroupAvatar = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => uploadGroupAvatar(groupId, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useAddGroupMembers = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (memberIds: string[]) => addGroupMembers(groupId, memberIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

export const useRemoveGroupMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => removeGroupMember(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

export const useSetGroupAdmin = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => setGroupAdmin(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

export const useLeaveGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => leaveGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

export const useDissolveGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => dissolveGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};
