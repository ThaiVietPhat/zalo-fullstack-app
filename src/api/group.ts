import { fetchAPI } from "@/lib/fetch";
import { MessageDto } from "./message";

// GroupMemberDto — khớp chính xác với BE GroupMemberDto.java
export interface GroupMemberDto {
  userId: string;       // BE dùng "userId", không phải "id"
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl: string | null;
  admin: boolean;       // BE dùng "admin", không phải "isAdmin"
  online: boolean;
  lastSeenText?: string;
}

// Helper để lấy tên đầy đủ của thành viên nhóm
export const getMemberName = (m: GroupMemberDto): string =>
  `${m.firstName || ''} ${m.lastName || ''}`.trim() || m.email;

// GroupDto — khớp chính xác với BE GroupDto.java
export interface GroupDto {
  id: string;
  name: string;
  description?: string;
  avatarUrl: string | null;
  createdById: string;      // BE trả về "createdById" không phải "adminId"
  memberCount: number;
  members: GroupMemberDto[];
  isAdmin: boolean;         // quyền của user hiện tại trong nhóm
  // Preview tin nhắn cuối
  lastMessage?: string;
  lastMessageTime?: string;
  lastMessageSenderName?: string;
  lastMessageType?: "TEXT" | "IMAGE" | "FILE" | "VIDEO" | "AUDIO" | "SYSTEM";
  unreadCount?: number;
}

export const getMyGroups = async (): Promise<GroupDto[]> => {
  return fetchAPI("/group");
};

export const getGroupById = async (groupId: string): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}`);
};

export const getGroupMessages = async (groupId: string, page = 0, size = 30): Promise<MessageDto[]> => {
  return fetchAPI(`/group/${groupId}/messages?page=${page}&size=${size}`);
};

export const createGroup = async (payload: { name: string; memberIds: string[] }): Promise<GroupDto> => {
  return fetchAPI("/group", {
    method: 'POST',
    body: JSON.stringify(payload)
  });
};

export const sendGroupMessage = async (groupId: string, payload: Partial<MessageDto>): Promise<MessageDto> => {
  return fetchAPI(`/group/${groupId}/messages`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
};

export const uploadGroupMedia = async (groupId: string, formData: FormData): Promise<MessageDto> => {
  return fetchAPI(`/group/${groupId}/upload-media`, {
    method: 'POST',
    body: formData
  });
};

export const recallGroupMessage = async (groupId: string, messageId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/messages/${messageId}/recall`, { method: 'DELETE' });
};

export const deleteGroupMessageForMe = async (groupId: string, messageId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/messages/${messageId}`, { method: 'DELETE' });
};

export const updateGroup = async (groupId: string, payload: { name: string }): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}`, { method: 'PUT', body: JSON.stringify(payload) });
};

export const uploadGroupAvatar = async (groupId: string, formData: FormData): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/avatar`, { method: 'POST', body: formData });
};

export const addGroupMembers = async (groupId: string, memberIds: string[]): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/members`, { method: 'POST', body: JSON.stringify({ userIds: memberIds }) });
};

export const removeGroupMember = async (groupId: string, userId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/members/${userId}`, { method: 'DELETE' });
};

export const setGroupAdmin = async (groupId: string, userId: string): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/members/${userId}/set-admin`, { method: 'PATCH' });
};

export const leaveGroup = async (groupId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/leave`, { method: 'DELETE' });
};

export const dissolveGroup = async (groupId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/dissolve`, { method: 'DELETE' });
};
