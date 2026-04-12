import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getMyGroups, createGroup, sendGroupMessage, uploadGroupMedia,
  getGroupById, getGroupMessages, recallGroupMessage, deleteGroupMessageForMe,
  updateGroup, uploadGroupAvatar, addGroupMembers, removeGroupMember,
  setGroupAdmin, leaveGroup, dissolveGroup,
} from "@/api/group";
import { MessageDto } from "@/api/message";
import { useAuthStore } from "@/store";

export const useMyGroups = () => {
  return useQuery({
    queryKey: ["groups"],
    queryFn: getMyGroups,
    staleTime: 60_000,
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
    staleTime: 60_000,
  });
};

export const useSendGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: Partial<MessageDto>) => sendGroupMessage(groupId, payload),
    onMutate: async (payload) => {
      const user = useAuthStore.getState().user;
      const tempId = 'temp-' + Date.now();

      // 1. Cập nhật Preview ngầm ở Home
      queryClient.setQueryData(["groups"], (old: any[] | undefined) => {
        if (!old) return old;
        const newList = [...old];
        const index = newList.findIndex(g => g.id === groupId);
        if (index !== -1) {
          newList[index] = {
            ...newList[index],
            lastMessage: payload.content,
            lastMessageTime: new Date().toISOString(),
            lastMessageSenderName: "Bạn"
          };
          newList.unshift(newList.splice(index, 1)[0]);
        }
        return newList;
      });

      // 2. Chèn tin nhắn tạm vào danh sách
      queryClient.setQueryData(["group-messages", groupId, 0], (old: any[] | undefined) => {
        const current = old || [];
        const newMessage = {
          id: tempId,
          content: payload.content,
          senderId: (user as any)?.id,
          senderName: (user as any)?.name || "Bạn",
          createdAt: new Date().toISOString(),
          state: 'SENDING',
          type: payload.type || 'TEXT',
          deleted: false
        };
        return [newMessage, ...current];
      });

      return { tempId };
    },
    onSuccess: (data, variables, context) => {
      queryClient.setQueryData(["group-messages", groupId, 0], (old: any[] | undefined) => {
        if (!old) return old;
        const alreadyExists = old.some(m => m.id === data.id);
        if (alreadyExists) return old.filter(m => m.id !== context?.tempId);
        return old.map(m => m.id === context?.tempId ? data : m);
      });
    },
  });
};

export const useUploadGroupMedia = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { formData: FormData, localUri: string, fileType: string, fileName: string }) =>
      uploadGroupMedia(groupId, data.formData),
    onMutate: async (variables) => {
      const user = useAuthStore.getState().user;
      const tempId = 'temp-media-' + Date.now();

      // 1. Cập nhật Preview Home
      queryClient.setQueryData(["groups"], (old: any[] | undefined) => {
        if (!old) return old;
        const newList = [...old];
        const index = newList.findIndex(g => g.id === groupId);
        if (index !== -1) {
          newList[index] = {
            ...newList[index],
            lastMessage: "[Hình ảnh/Tệp tin]",
            lastMessageTime: new Date().toISOString(),
            lastMessageSenderName: "Bạn"
          };
          newList.unshift(newList.splice(index, 1)[0]);
        }
        return newList;
      });

      // 2. Chèn tin nhắn media tạm thời
      queryClient.setQueryData(["group-messages", groupId, 0], (old: any[] | undefined) => {
        const current = old || [];
        const isImage = variables.fileType.startsWith('image');
        const newMessage = {
          id: tempId,
          content: variables.fileName,
          mediaUrl: variables.localUri,
          senderId: (user as any)?.id,
          senderName: (user as any)?.name || "Bạn",
          createdAt: new Date().toISOString(),
          state: 'SENDING',
          type: isImage ? 'IMAGE' : (variables.fileType.startsWith('video') ? 'VIDEO' : 'FILE'),
          deleted: false
        };
        return [newMessage, ...current];
      });

      return { tempId };
    },
    onSuccess: (data, variables, context) => {
      queryClient.setQueryData(["group-messages", groupId, 0], (old: any[] | undefined) => {
        if (!old) return old;
        const alreadyExists = old.some(m => m.id === data.id);
        if (alreadyExists) return old.filter(m => m.id !== context?.tempId);
        return old.map(m => m.id === context?.tempId ? data : m);
      });
    },
  });
};

export const useRecallGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => recallGroupMessage(groupId, messageId),
    onMutate: async (messageId) => {
      const user = useAuthStore.getState().user;
      const senderName = (user as any)?.name || (user as any)?.firstName || "Bạn";
      queryClient.setQueryData(['group-messages', groupId, 0], (old: any[] | undefined) => {
        if (!old) return old;
        return old.map((m: any) => m.id === messageId
          ? { ...m, deleted: true, content: `Tin nhắn đã được ${senderName} thu hồi`, text: `Tin nhắn đã được ${senderName} thu hồi` }
          : m
        );
      });
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ['group-messages', groupId, 0] });
    },
  });
};

export const useDeleteGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => deleteGroupMessageForMe(groupId, messageId),
    onMutate: async (messageId) => {
      queryClient.setQueryData(['group-messages', groupId, 0], (old: any[] | undefined) => {
        if (!old) return old;
        return old.filter((m: any) => m.id !== messageId);
      });
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: ['group-messages', groupId, 0] });
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
