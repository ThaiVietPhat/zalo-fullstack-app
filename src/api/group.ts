import { fetchAPI } from "@/lib/fetch";
import { UserDto } from "./user";
import { MessageDto } from "./message";

export interface GroupDto {
  id: string;
  name: string;
  avatarUrl: string | null;
  adminId: string;
  members: UserDto[];
  createdAt: string;
  // Extracted fields for UI convenience matching ChatDto format partially
  lastMessage?: string;
  lastMessageTime?: string;
  lastMessageType?: "TEXT" | "IMAGE" | "FILE" | "VIDEO" | "AUDIO" | "SYSTEM";
  unreadCount?: number;
}

export const getMyGroups = async (): Promise<GroupDto[]> => {
  return fetchAPI("/group");
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
