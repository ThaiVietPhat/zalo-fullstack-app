// hooks/useGroup.ts — đồng bộ GroupMessageDto.java (createdDate, isMine, deleted)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getMyGroups,
  getGroupById,
  createGroup,
  updateGroup,
  addGroupMembers,
  removeGroupMember,
  leaveGroup,
  setMemberAsAdmin,
  dissolveGroup,
  getGroupMessages,
  sendGroupMessage,
  uploadGroupMedia,
  recallGroupMessage,
  GroupDto,
  GroupMessageDto,
  CreateGroupPayload,
  UpdateGroupPayload,
  AddMemberPayload,
  SendGroupMessagePayload,
} from "@/api/group";

/** Lấy danh sách nhóm của tôi */
export const useMyGroups = () => {
  return useQuery<GroupDto[]>({
    queryKey: ["groups"],
    queryFn: getMyGroups,
    staleTime: 30_000,
  });
};

/** Lấy chi tiết 1 nhóm */
export const useGroupById = (groupId: string | null) => {
  return useQuery<GroupDto>({
    queryKey: ["group", groupId],
    queryFn: () => getGroupById(groupId!),
    enabled: !!groupId,
  });
};

/** Tạo nhóm mới */
export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateGroupPayload) => createGroup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

/** Cập nhật nhóm (chỉ admin) */
export const useUpdateGroup = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateGroupPayload) => updateGroup(groupId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

/** Thêm thành viên (chỉ admin) */
export const useAddGroupMembers = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    // AddMemberPayload dùng field "userIds" (không phải memberIds)
    mutationFn: (data: AddMemberPayload) => addGroupMembers(groupId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

/** Xóa thành viên (chỉ admin) */
export const useRemoveGroupMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => removeGroupMember(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

/** Rời nhóm */
export const useLeaveGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => leaveGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

/** Cấp quyền admin */
export const useSetMemberAsAdmin = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => setMemberAsAdmin(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
};

/** Giải tán nhóm (chỉ admin) */
export const useDissolveGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => dissolveGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

/** Lấy tin nhắn nhóm (phân trang) */
export const useGroupMessages = (groupId: string | null, page = 0) => {
  return useQuery<GroupMessageDto[]>({
    queryKey: ["group-messages", groupId, page],
    queryFn: () => getGroupMessages(groupId!, page),
    enabled: !!groupId,
    staleTime: 10_000,
  });
};

/** Gửi tin nhắn nhóm */
export const useSendGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SendGroupMessagePayload) => sendGroupMessage(groupId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
};

/** Upload media nhóm */
export const useUploadGroupMedia = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => uploadGroupMedia(groupId, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
    },
  });
};

/** Thu hồi tin nhắn nhóm */
export const useRecallGroupMessage = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) => recallGroupMessage(groupId, messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["group-messages", groupId] });
    },
  });
};
