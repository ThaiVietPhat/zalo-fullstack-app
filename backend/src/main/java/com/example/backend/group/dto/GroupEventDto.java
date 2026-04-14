package com.example.backend.group.dto;

import java.util.List;
import java.util.UUID;

public record GroupEventDto(
        String type,
        UUID groupId,
        UUID targetUserId,
        UUID actorUserId,
        GroupDto groupSnapshot,
        List<GroupMessageDto> pinnedMessages
) {
    public static final String MEMBER_REMOVED  = "MEMBER_REMOVED";
    public static final String MEMBER_LEFT     = "MEMBER_LEFT";
    public static final String MEMBER_ADDED    = "MEMBER_ADDED";
    public static final String ADMIN_CHANGED   = "ADMIN_CHANGED";
    public static final String GROUP_UPDATED   = "GROUP_UPDATED";
    public static final String MESSAGE_PINNED  = "MESSAGE_PINNED";
    public static final String MESSAGE_UNPINNED = "MESSAGE_UNPINNED";
    public static final String GROUP_DISSOLVED  = "GROUP_DISSOLVED";
}
