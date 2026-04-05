import { TextInputProps, TouchableOpacityProps } from "react-native";

// ─── UI Component Props ────────────────────────────────────────────────────────

export interface ButtonProps extends TouchableOpacityProps {
  title: string;
  bgVariant?: "primary" | "secondary" | "danger" | "outline" | "success" | "amber" | "red";
  textVariant?: "primary" | "default" | "secondary" | "danger" | "success" | "amber" | "red";
  IconLeft?: React.ComponentType<any>;
  IconRight?: React.ComponentType<any>;
  className?: string;
  loading?: boolean;
}

export interface InputFieldProps extends TextInputProps {
  label?: string;
  icon?: any;
  iconRight?: any;
  secureTextEntry?: boolean;
  labelStyle?: string;
  containerStyle?: string;
  inputStyle?: string;
  iconStyle?: string;
  className?: string;
  error?: string;
}

// ─── Auth / User ──────────────────────────────────────────────────────────────

export interface User {
  id: string;
  name: string;
  role: string;
  email?: string;
  avatar?: string;
}

export interface AuthStore {
  token: string | null;
  user: User | null;
  hasHydrated: boolean;
  setAuth: (token: string, user: User) => void;
  setHasHydrated: (state: boolean) => void;
  logout: () => void;
}

// ─── Re-export API types for convenience ─────────────────────────────────────
// Dùng trực tiếp từ api/*.ts để luôn đồng bộ với BE DTO

// MessageState từ MessageDto.java
export type MessageState = "SENT" | "DELIVERED" | "SEEN";

// MessageType từ MessageDto.java + GroupMessageDto.java
export type MessageType = "TEXT" | "IMAGE" | "VIDEO" | "FILE" | "AUDIO" | "VOICE";

// ChatMessage — format dùng cho UI MessageItem (đã map từ MessageDto)
export interface ChatMessage {
  id: string;
  senderId: string;
  text: string;
  image?: string;
  time: string;
  type: "TEXT" | "IMAGE";
  state?: MessageState;
  reactions?: Array<{ emoji: string; userId: string }>;
}
