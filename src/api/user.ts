// api/user.ts — đồng bộ với UserController & UserDto
import { fetchAPI } from "@/lib/fetch";

// ─── Types (khớp UserDto.java) ────────────────────────────────────────────────
export interface UserDto {
  id: string;           // UUID
  firstName: string;
  lastName: string;
  email: string;
  lastSeen?: string;    // LocalDateTime → ISO string
  online: boolean;
  lastSeenText?: string;
  role: string;
  banned: boolean;
  avatarUrl?: string;
}

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

// ─── API calls ────────────────────────────────────────────────────────────────

/** GET /api/v1/user — Lấy tất cả user trừ bản thân */
export const getAllUsers = async (): Promise<UserDto[]> => {
  return fetchAPI("/user");
};

/** GET /api/v1/user/me — Lấy profile của mình */
export const getMyProfile = async (): Promise<UserDto> => {
  return fetchAPI("/user/me");
};

/** PUT /api/v1/user/me — Cập nhật profile */
export const updateMyProfile = async (data: UpdateProfilePayload): Promise<UserDto> => {
  return fetchAPI("/user/me", {
    method: "PUT",
    body: JSON.stringify(data),
  });
};

/** PATCH /api/v1/user/me/password — Đổi mật khẩu */
export const changePassword = async (data: ChangePasswordPayload): Promise<void> => {
  await fetchAPI("/user/me/password", {
    method: "PATCH",
    body: JSON.stringify(data),
  });
};

/** POST /api/v1/user/me/avatar — Upload avatar (multipart/form-data, field: "file") */
export const uploadAvatar = async (formData: FormData): Promise<UserDto> => {
  return fetchAPI("/user/me/avatar", {
    method: "POST",
    body: formData,
  });
};

/** GET /api/v1/user/search?keyword=... — Tìm kiếm user theo tên/email */
export const searchUsers = async (keyword: string): Promise<UserDto[]> => {
  return fetchAPI(`/user/search?keyword=${encodeURIComponent(keyword)}`);
};
