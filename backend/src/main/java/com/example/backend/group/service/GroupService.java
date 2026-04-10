package com.example.backend.group.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.file.service.FileStorageService;
import com.example.backend.group.dto.GroupDto;
import com.example.backend.group.dto.GroupMemberDto;
import com.example.backend.group.dto.GroupMessageDto;
import com.example.backend.group.dto.GroupRequest;
import com.example.backend.group.entity.Group;
import com.example.backend.group.entity.GroupMember;
import com.example.backend.group.entity.GroupMessage;
import com.example.backend.group.repository.GroupMemberRepository;
import com.example.backend.group.repository.GroupMessageReactionRepository;
import com.example.backend.group.repository.GroupMessageRepository;
import com.example.backend.group.repository.GroupRepository;
import com.example.backend.messaging.enums.MessageType;
import com.example.backend.reaction.dto.ReactionDto;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.shared.exception.UnauthorizedException;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMessageReactionRepository groupMessageReactionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileStorageService fileStorageService;

    // ─── Tạo nhóm ────────────────────────────────────────────────────────────

    @Transactional
    public GroupDto createGroup(GroupRequest.Create request, Authentication currentUser) {
        User creator = getUser(currentUser);

        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreatedBy(creator);

        Group saved = groupRepository.save(group);

        // Thêm người tạo là admin
        saved.getMembers().add(GroupMember.of(saved, creator, true));

        // Thêm các thành viên được mời
        for (UUID memberId : request.getMemberIds()) {
            if (memberId.equals(creator.getId())) continue; // bỏ qua nếu trùng creator
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
            saved.getMembers().add(GroupMember.of(saved, member, false));
        }

        groupRepository.save(saved);
        log.info("Group '{}' created by {}", saved.getName(), creator.getEmail());
        return toGroupDto(saved, creator.getId());
    }

    // ─── Lấy danh sách nhóm của user ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupDto> getMyGroups(Authentication currentUser) {
        User user = getUser(currentUser);
        return groupRepository.findAllGroupsByUserId(user.getId())
                .stream()
                .map(g -> toGroupDto(g, user.getId()))
                .collect(Collectors.toList());
    }

    // ─── Lấy chi tiết nhóm ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GroupDto getGroupById(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());
        return toGroupDto(group, user.getId());
    }

    // ─── Cập nhật thông tin nhóm (chỉ admin) ─────────────────────────────────

    @Transactional
    public GroupDto updateGroup(UUID groupId, GroupRequest.Update request, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        if (request.getAvatarUrl() != null) group.setAvatarUrl(request.getAvatarUrl());

        groupRepository.save(group);
        log.info("Group '{}' updated by {}", group.getName(), user.getEmail());
        return toGroupDto(group, user.getId());
    }

    // ─── Upload avatar nhóm (chỉ admin) ──────────────────────────────────────

    @Transactional
    public GroupDto uploadGroupAvatar(UUID groupId, MultipartFile file, Authentication currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh");
        }

        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        String filename = fileStorageService.saveFile(file);
        group.setAvatarUrl(filename);

        groupRepository.save(group);
        log.info("Group '{}' avatar updated by {}", group.getName(), user.getEmail());
        return toGroupDto(group, user.getId());
    }

    // ─── Thêm thành viên (chỉ admin) ─────────────────────────────────────────

    @Transactional
    public GroupDto addMembers(UUID groupId, GroupRequest.AddMember request, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        for (UUID userId : request.getUserIds()) {
            if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) continue;
            User newMember = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            group.getMembers().add(GroupMember.of(group, newMember, false));
        }

        groupRepository.save(group);
        log.info("{} members added to group '{}'", request.getUserIds().size(), group.getName());
        return toGroupDto(group, user.getId());
    }

    // ─── Xóa thành viên (chỉ admin, không thể xóa chính mình) ───────────────

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckAdmin(groupId, user.getId());

        if (user.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Admin không thể tự xóa mình, hãy dùng rời nhóm");
        }

        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        groupMemberRepository.delete(member);
        log.info("User {} removed from group {}", targetUserId, groupId);
    }

    // ─── Rời nhóm ────────────────────────────────────────────────────────────

    @Transactional
    public void leaveGroup(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());

        // Nếu admin rời nhóm và còn thành viên khác → chuyển quyền admin cho người vào sớm nhất
        if (group.isAdmin(user.getId()) && group.getMembers().size() > 1) {
            group.getMembers().stream()
                    .filter(m -> !m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .ifPresent(m -> m.setAdmin(true));
        }

        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải thành viên nhóm này"));

        groupMemberRepository.delete(member);

        // Xóa nhóm nếu không còn ai
        if (groupMemberRepository.countByGroupId(groupId) == 0) {
            groupRepository.delete(group);
            log.info("Group {} deleted — no members left", groupId);
        } else {
            log.info("User {} left group {}", user.getEmail(), groupId);
        }
    }

    // ─── Gán quyền admin cho thành viên (chỉ admin) ───────────────────────────

    @Transactional
    public GroupDto setMemberAsAdmin(UUID groupId, UUID targetUserId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        if (user.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Bạn đã là admin rồi");
        }

        GroupMember member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        if (member.isAdmin()) {
            throw new IllegalArgumentException("Thành viên này đã là admin");
        }

        member.setAdmin(true);
        groupMemberRepository.save(member);
        log.info("User {} promoted to admin in group {}", targetUserId, groupId);

        return toGroupDto(group, user.getId());
    }

    // ─── Giải tán nhóm (chỉ admin) ────────────────────────────────────────────

    @Transactional
    public void dissolveGroup(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        // Xóa tất cả thành viên
        groupMemberRepository.deleteByGroupId(groupId);

        // Xóa nhóm
        groupRepository.delete(group);
        log.info("Group {} dissolved by admin {}", groupId, user.getEmail());
    }

    // ─── Gửi tin nhắn nhóm ───────────────────────────────────────────────────

    @Transactional
    public GroupMessageDto sendMessage(UUID groupId, GroupRequest.SendMessage request, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());

        GroupMessage msg = new GroupMessage();
        msg.setGroup(group);
        msg.setSender(user);
        msg.setContent(request.getContent());
        msg.setType(request.getType() != null ? request.getType() : MessageType.TEXT);

        GroupMessage saved = groupMessageRepository.save(msg);
        GroupMessageDto dto = toMessageDto(saved, user.getId());

        // Broadcast qua WebSocket đến tất cả thành viên nhóm
        messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
        log.debug("Message sent to group {} by {}", groupId, user.getEmail());

        return dto;
    }

    // ─── Upload media vào nhóm ────────────────────────────────────────────────

    @Transactional
    public GroupMessageDto uploadGroupMediaMessage(UUID groupId, MultipartFile file, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        MessageType messageType;
        if (contentType.startsWith("image/")) {
            messageType = MessageType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            messageType = MessageType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            messageType = MessageType.AUDIO;
        } else {
            messageType = MessageType.FILE;
        }

        String filePath = fileStorageService.saveFile(file);

        GroupMessage msg = new GroupMessage();
        msg.setGroup(group);
        msg.setSender(user);
        msg.setContent(filePath);
        msg.setType(messageType);
        msg.setFileName(file.getOriginalFilename());

        GroupMessage saved = groupMessageRepository.save(msg);
        GroupMessageDto dto = toMessageDto(saved, user.getId());

        messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
        log.info("Media message sent to group {} by {}", groupId, user.getEmail());
        return dto;
    }

    // ─── Thu hồi tin nhắn nhóm ───────────────────────────────────────────────

    @Transactional
    public void recallGroupMessage(UUID messageId, Authentication currentUser) {
        User user = getUser(currentUser);

        GroupMessage msg = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn không tồn tại: " + messageId));

        boolean isSender = msg.getSender().getId().equals(user.getId());
        boolean isAdmin = msg.getGroup().isAdmin(user.getId());

        if (!isSender && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền thu hồi tin nhắn này");
        }

        if (msg.isDeleted()) {
            throw new IllegalArgumentException("Tin nhắn đã được thu hồi trước đó");
        }

        msg.setDeleted(true);
        groupMessageRepository.save(msg);

        GroupMessageDto dto = toMessageDto(msg, user.getId());
        messagingTemplate.convertAndSend("/topic/group/" + msg.getGroup().getId(), dto);
        log.info("Group message {} recalled by {}", messageId, user.getEmail());
    }

    // ─── Lấy tin nhắn nhóm (phân trang) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupMessageDto> getMessages(UUID groupId, int page, int size, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckMember(groupId, user.getId());

        List<GroupMessage> messages = groupMessageRepository
                .findByGroupIdAndDeletedFalseOrderByCreatedDateAsc(groupId, PageRequest.of(page, size))
                .getContent();

        List<GroupMessageDto> dtos = messages.stream()
                .map(m -> toMessageDto(m, user.getId()))
                .collect(Collectors.toList());

        // Gắn reactions vào từng tin nhắn (batch query tránh N+1)
        List<UUID> msgIds = messages.stream().map(GroupMessage::getId).collect(Collectors.toList());
        if (!msgIds.isEmpty()) {
            Map<UUID, List<ReactionDto>> reactionsByMsgId = groupMessageReactionRepository
                    .findByGroupMessageIdIn(msgIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getGroupMessage().getId(),
                            Collectors.mapping(r -> ReactionDto.builder()
                                    .id(r.getId())
                                    .userId(r.getUser().getId())
                                    .userFullName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                                    .emoji(r.getEmoji())
                                    .createdDate(r.getCreatedDate())
                                    .build(), Collectors.toList())
                    ));

            dtos.forEach(dto -> dto.setReactions(reactionsByMsgId.getOrDefault(dto.getId(), List.of())));
        }

        return dtos;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Group getGroupAndCheckMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại với id: " + groupId));
        if (!group.isMember(userId)) {
            throw new UnauthorizedException("Bạn không phải thành viên nhóm này");
        }
        return group;
    }

    private Group getGroupAndCheckAdmin(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại với id: " + groupId));
        if (!group.isAdmin(userId)) {
            throw new UnauthorizedException("Chỉ admin mới có quyền thực hiện thao tác này");
        }
        return group;
    }

    private GroupDto toGroupDto(Group group, UUID currentUserId) {
        List<GroupMemberDto> memberDtos = group.getMembers().stream()
                .map(m -> GroupMemberDto.builder()
                        .userId(m.getUser().getId())
                        .firstName(m.getUser().getFirstName())
                        .lastName(m.getUser().getLastName())
                        .email(m.getUser().getEmail())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .admin(m.isAdmin())
                        .online(m.getUser().isUserOnline())
                        .lastSeenText(m.getUser().getLastSeenText())
                        .build())
                .collect(Collectors.toList());

        GroupDto dto = GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .createdById(group.getCreatedBy().getId())
                .memberCount(group.getMembers().size())
                .members(memberDtos)
                .isAdmin(group.isAdmin(currentUserId))
                .build();

        // Gắn preview tin nhắn cuối
        groupMessageRepository.findTop1ByGroupIdOrderByCreatedDateDesc(group.getId())
                .ifPresent(last -> {
                    dto.setLastMessage(last.getContent());
                    dto.setLastMessageType(last.getType());
                    dto.setLastMessageTime(last.getCreatedDate());
                    dto.setLastMessageSenderName(
                            last.getSender().getFirstName() + " " + last.getSender().getLastName()
                    );
                });

        return dto;
    }

    private GroupMessageDto toMessageDto(GroupMessage msg, UUID currentUserId) {
        boolean isMedia = msg.getType() != null && msg.getType() != com.example.backend.messaging.enums.MessageType.TEXT;
        String rawContent = msg.getContent();
        String mediaUrl = (!msg.isDeleted() && isMedia && rawContent != null)
                ? fileStorageService.generatePresignedUrl(rawContent)
                : null;
        String content = msg.isDeleted() ? null : (isMedia ? null : msg.getContent());

        return GroupMessageDto.builder()
                .id(msg.getId())
                .content(content)
                .mediaUrl(mediaUrl)
                .type(msg.getType())
                .groupId(msg.getGroup().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFirstName() + " " + msg.getSender().getLastName())
                .isMine(msg.getSender().getId().equals(currentUserId))
                .createdDate(msg.getCreatedDate())
                .deleted(msg.isDeleted())
                .fileName(msg.getFileName())
                .build();
    }
}