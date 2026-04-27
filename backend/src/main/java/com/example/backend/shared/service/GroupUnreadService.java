package com.example.backend.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Quan ly so tin nhan chua doc cua tung user trong moi nhom (Redis HASH).
 *
 * Key: "group:unread:{groupId}"  (HASH)
 * Field: "{userId}" → so luong tin nhan chua doc
 *
 * - Increment: khi co tin nhan moi trong nhom, tang count cho tat ca member tru sender
 * - Clear: khi user mo nhom (goi GET /messages), reset count cua user do ve 0
 * - Get: khi load danh sach nhom cua user, doc count de tra ve trong GroupDto
 */
@Service
@RequiredArgsConstructor
public class GroupUnreadService {

    private static final String KEY_PREFIX = "group:unread:";

    private final StringRedisTemplate redisTemplate;

    /** Tang count cho mot user trong mot nhom. */
    public void increment(UUID groupId, UUID userId) {
        redisTemplate.opsForHash().increment(KEY_PREFIX + groupId, userId.toString(), 1);
    }

    /** Reset count cua user trong nhom ve 0 (khi ho mo nhom). */
    public void clear(UUID groupId, UUID userId) {
        redisTemplate.opsForHash().delete(KEY_PREFIX + groupId, userId.toString());
    }

    /** Lay count cua user trong nhom, 0 neu chua co. */
    public int getCount(UUID groupId, UUID userId) {
        Object val = redisTemplate.opsForHash().get(KEY_PREFIX + groupId, userId.toString());
        if (val == null) return 0;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }

    /** Xoa toan bo hash cua nhom (khi nhom bi giai tan). */
    public void deleteGroup(UUID groupId) {
        redisTemplate.delete(KEY_PREFIX + groupId);
    }
}
