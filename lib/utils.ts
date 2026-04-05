// lib/utils.ts — Zalo Clone utilities
import { format } from 'date-fns-tz';
import { formatInTimeZone } from 'date-fns-tz';

const VIETNAM_TIMEZONE = 'Asia/Ho_Chi_Minh';

/** Format duration in minutes to human-readable string */
export function formatTime(minutes: number): string {
  const formattedMinutes = Math.round(minutes) || 0;
  if (formattedMinutes < 60) return `${formattedMinutes} phút`;
  const hours = Math.floor(formattedMinutes / 60);
  const remainingMinutes = formattedMinutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

/** Format ISO date string to dd/MM/yyyy */
export function formatDateVN(dateString: string): string {
  try {
    return formatInTimeZone(new Date(dateString), VIETNAM_TIMEZONE, 'dd/MM/yyyy');
  } catch {
    return dateString;
  }
}

/** Format ISO date string to dd/MM/yyyy HH:mm */
export function formatDateTimeVN(dateString: string): string {
  try {
    return formatInTimeZone(new Date(dateString), VIETNAM_TIMEZONE, 'dd/MM/yyyy HH:mm');
  } catch {
    return dateString;
  }
}

/** Format ISO date string to HH:mm */
export function formatTimeVN(dateString: string): string {
  try {
    return formatInTimeZone(new Date(dateString), VIETNAM_TIMEZONE, 'HH:mm');
  } catch {
    return '';
  }
}

/** Format message timestamp — show time if today, else show date */
export function formatMessageTime(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays === 0) {
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    return 'Hôm qua';
  } else {
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
  }
}

/** Format full name from firstName + lastName */
export function formatFullName(firstName?: string, lastName?: string): string {
  return `${firstName || ''} ${lastName || ''}`.trim() || 'Người dùng';
}

/** Generate DiceBear avatar URL from a seed string */
export function getAvatarUrl(seed: string, fallback?: string): string {
  if (fallback) return getImageUrl(fallback) || fallback;
  return `https://api.dicebear.com/9.x/avataaars/png?seed=${encodeURIComponent(seed)}`;
}

/** Resolve relative API media paths into full URLs */
export function getImageUrl(path?: string): string | undefined {
  if (!path) return undefined;
  if (path.startsWith('http')) return path;
  
  // Lấy baseUrl từ .env (ví dụ: http://192.168.1.3:8080/api/v1)
  const baseUrl = process.env.EXPO_PUBLIC_SERVER_URL || "";
  // Tách lấy host (ví dụ: http://192.168.1.3:8080)
  const host = baseUrl.split('/api/v1')[0];
  
  return `${host}${path}`;
}