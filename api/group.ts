// api/group.ts — đồng bộ với GroupController + GroupDto/GroupRequest/GroupMessageDto
import { fetchAPI } from "@/lib/fetch";
import { MessageType } from "./message";

// ─── Types (khớp GroupMemberDto.java) ────────────────────────────────────────
export interface GroupMemberDto {
  id: string;        // UUID
  firstName: string;
  lastName: string;
  avatarUrl?: string;
  role: "ADMIN" | "MEMBER";
  online?: boolean;
}

// ─── Types (khớp GroupMessageDto.java) ────────────────────────────────────────
export interface ReactionDto {
  emoji: string;
  userId: string;
  userName?: string;
}

export interface GroupMessageDto {
  id: string;            // UUID
  content: string;
  mediaUrl?: string;
  type: MessageType;
  groupId: string;       // UUID
  senderId: string;      // UUID
  senderName: string;
  isMine: boolean;
  createdDate: string;   // LocalDateTime → ISO string
  deleted: boolean;
  reactions?: ReactionDto[];
}

// ─── Types (khớp GroupDto.java) ───────────────────────────────────────────────
export interface GroupDto {
  id: string;            // UUID
  name: string;
  description?: string;
  avatarUrl?: string;
  createdById: string;   // UUID
  memberCount: number;
  members: GroupMemberDto[];

  // Preview tin nhắn cuối
  lastMessage?: string;
  lastMessageType?: MessageType;
  lastMessageTime?: string;  // ISO string
  lastMessageSenderName?: string;

  // Quyền user hiện tại
  isAdmin: boolean;
}

// ─── Request Payloads (khớp GroupRequest.java) ────────────────────────────────
export interface CreateGroupPayload {
  name: string;
  description?: string;
  memberIds: string[];   // UUID[] — không bao gồm người tạo
}

export interface UpdateGroupPayload {
  name?: string;
  description?: string;
  avatarUrl?: string;
}

export interface AddMemberPayload {
  userIds: string[];     // UUID[] — field tên "userIds" (không phải memberIds)
}

export interface SendGroupMessagePayload {
  content: string;
  type?: MessageType;    // default TEXT
}

// ─── API calls ────────────────────────────────────────────────────────────────

/** POST /api/v1/group → 201 GroupDto */
export const createGroup = async (data: CreateGroupPayload): Promise<GroupDto> => {
  return fetchAPI("/group", {
    method: "POST",
    body: JSON.stringify(data),
  });
};

/** GET /api/v1/group — Lấy danh sách nhóm của tôi */
export const getMyGroups = async (): Promise<GroupDto[]> => {
  return fetchAPI("/group");
};

/** GET /api/v1/group/{groupId} — Chi tiết nhóm */
export const getGroupById = async (groupId: string): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}`);
};

/** PUT /api/v1/group/{groupId} — Cập nhật nhóm (chỉ admin) */
export const updateGroup = async (groupId: string, data: UpdateGroupPayload): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
};

/** POST /api/v1/group/{groupId}/avatar — Upload avatar nhóm (field: "file") */
export const uploadGroupAvatar = async (groupId: string, formData: FormData): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/avatar`, {
    method: "POST",
    body: formData,
  });
};

/** POST /api/v1/group/{groupId}/members — Thêm thành viên */
export const addGroupMembers = async (groupId: string, data: AddMemberPayload): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/members`, {
    method: "POST",
    body: JSON.stringify(data),
  });
};

/** DELETE /api/v1/group/{groupId}/members/{userId} — Xóa thành viên (chỉ admin) */
export const removeGroupMember = async (groupId: string, userId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/members/${userId}`, {
    method: "DELETE",
  });
};

/** DELETE /api/v1/group/{groupId}/leave — Rời nhóm */
export const leaveGroup = async (groupId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/leave`, {
    method: "DELETE",
  });
};

/** PATCH /api/v1/group/{groupId}/members/{userId}/set-admin — Cấp quyền admin */
export const setMemberAsAdmin = async (groupId: string, userId: string): Promise<GroupDto> => {
  return fetchAPI(`/group/${groupId}/members/${userId}/set-admin`, {
    method: "PATCH",
  });
};

/** DELETE /api/v1/group/{groupId}/dissolve — Giải tán nhóm (chỉ admin) */
export const dissolveGroup = async (groupId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/dissolve`, {
    method: "DELETE",
  });
};

/** POST /api/v1/group/{groupId}/messages → 201 GroupMessageDto */
export const sendGroupMessage = async (groupId: string, data: SendGroupMessagePayload): Promise<GroupMessageDto> => {
  return fetchAPI(`/group/${groupId}/messages`, {
    method: "POST",
    body: JSON.stringify(data),
  });
};

/** POST /api/v1/group/{groupId}/upload-media → 201 GroupMessageDto (field: "file") */
export const uploadGroupMedia = async (groupId: string, formData: FormData): Promise<GroupMessageDto> => {
  return fetchAPI(`/group/${groupId}/upload-media`, {
    method: "POST",
    body: formData,
  });
};

/** DELETE /api/v1/group/{groupId}/messages/{messageId}/recall — Thu hồi tin nhắn */
export const recallGroupMessage = async (groupId: string, messageId: string): Promise<void> => {
  await fetchAPI(`/group/${groupId}/messages/${messageId}/recall`, {
    method: "DELETE",
  });
};

/** GET /api/v1/group/{groupId}/messages?page=0&size=30 — Lấy tin nhắn nhóm */
export const getGroupMessages = async (
  groupId: string,
  page = 0,
  size = 30
): Promise<GroupMessageDto[]> => {
  return fetchAPI(`/group/${groupId}/messages?page=${page}&size=${size}`);
};
