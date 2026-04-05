package com.example.backend.admin.service;

import com.example.backend.group.entity.Group;
import com.example.backend.user.entity.User;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.admin.dto.AdminGroupDto;
import com.example.backend.admin.dto.AdminStatsDto;
import com.example.backend.admin.dto.AdminUserDto;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.messaging.repository.MessageRepository;
import com.example.backend.group.repository.GroupMessageRepository;
import com.example.backend.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupRepository groupRepository;

    // ─── Quản lý User ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdDate").descending()))
                .map(this::toAdminUserDto);
    }

    @Transactional
    public AdminUserDto banUser(UUID userId) {
        User user = getUser(userId);
        user.setBanned(true);
        userRepository.save(user);
        log.info("User {} banned", userId);
        return toAdminUserDto(user);
    }

    @Transactional
    public AdminUserDto unbanUser(UUID userId) {
        User user = getUser(userId);
        user.setBanned(false);
        userRepository.save(user);
        log.info("User {} unbanned", userId);
        return toAdminUserDto(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        // Soft delete: đánh dấu role = DELETED, banned = true
        user.setRole("DELETED");
        user.setBanned(true);
        userRepository.save(user);
        log.info("User {} soft-deleted by admin", userId);
    }

    @Transactional
    public AdminUserDto promoteToAdmin(UUID userId) {
        User user = getUser(userId);
        user.setRole("ADMIN");
        userRepository.save(user);
        log.info("User {} promoted to ADMIN", userId);
        return toAdminUserDto(user);
    }

    @Transactional
    public AdminUserDto demoteToUser(UUID userId) {
        User user = getUser(userId);
        user.setRole("USER");
        userRepository.save(user);
        log.info("User {} demoted to USER", userId);
        return toAdminUserDto(user);
    }

    // ─── Quản lý Group ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminGroupDto> getAllGroups(int page, int size) {
        return groupRepository.findAll(PageRequest.of(page, size, Sort.by("createdDate").descending()))
                .map(this::toAdminGroupDto);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        groupRepository.delete(group);
        log.info("Group {} deleted by admin", groupId);
    }

    // ─── Thống kê ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalUsers    = userRepository.count();
        long totalGroups   = groupRepository.count();
        long onlineUsers   = userRepository.countByOnlineTrue();
        long bannedUsers   = userRepository.countByBannedTrue();
        long totalMessages = messageRepository.countByDeletedFalse()
                           + groupMessageRepository.countByDeletedFalse();

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Daily messages: merge 1-1 và group
        Map<String, Long> dailyMap = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        messageRepository.countDailyMessages(since).forEach(row -> {
            String date = row[0].toString().substring(0, 10);
            dailyMap.merge(date, ((Number) row[1]).longValue(), Long::sum);
        });
        groupMessageRepository.countDailyGroupMessages(since).forEach(row -> {
            String date = row[0].toString().substring(0, 10);
            dailyMap.merge(date, ((Number) row[1]).longValue(), Long::sum);
        });

        List<AdminStatsDto.DailyCountDto> dailyCounts = dailyMap.entrySet().stream()
                .map(e -> AdminStatsDto.DailyCountDto.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // Top active users: merge 1-1 và group message counts
        Map<UUID, Long> msgCountMap = new HashMap<>();
        messageRepository.findTopSenderIds(PageRequest.of(0, 50)).forEach(row -> {
            UUID uid = UUID.fromString(row[0].toString());
            msgCountMap.merge(uid, ((Number) row[1]).longValue(), Long::sum);
        });
        groupMessageRepository.findTopGroupSenderIds(PageRequest.of(0, 50)).forEach(row -> {
            UUID uid = UUID.fromString(row[0].toString());
            msgCountMap.merge(uid, ((Number) row[1]).longValue(), Long::sum);
        });

        List<UUID> topIds = msgCountMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<UUID, User> userMap = userRepository.findByIdIn(topIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminStatsDto.TopUserDto> topUsers = topIds.stream()
                .filter(userMap::containsKey)
                .map(uid -> AdminStatsDto.TopUserDto.builder()
                        .userId(uid)
                        .fullName(userMap.get(uid).getFirstName() + " " + userMap.get(uid).getLastName())
                        .messageCount(msgCountMap.get(uid))
                        .build())
                .collect(Collectors.toList());

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalMessages(totalMessages)
                .totalGroups(totalGroups)
                .onlineUsers(onlineUsers)
                .bannedUsers(bannedUsers)
                .dailyMessageCounts(dailyCounts)
                .topActiveUsers(topUsers)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private AdminUserDto toAdminUserDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .banned(user.isBanned())
                .online(user.isOnline())
                .lastSeen(user.getLastSeen())
                .createdDate(user.getCreatedDate())
                .build();
    }

    private AdminGroupDto toAdminGroupDto(Group group) {
        return AdminGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(group.getMembers().size())
                .createdById(group.getCreatedBy().getId())
                .createdByName(group.getCreatedBy().getFirstName() + " " + group.getCreatedBy().getLastName())
                .createdDate(group.getCreatedDate())
                .build();
    }
}
