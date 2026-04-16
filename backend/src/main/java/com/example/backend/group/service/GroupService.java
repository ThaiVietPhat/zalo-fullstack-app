package com.example.backend.group.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.example.backend.group.dto.GroupEventDto;
import com.example.backend.group.dto.GroupJoinRequestDto;
import com.example.backend.group.dto.GroupMemberDto;
import com.example.backend.group.dto.GroupMessageDto;
import com.example.backend.group.dto.GroupRequest;
import com.example.backend.group.entity.Group;
import com.example.backend.group.entity.GroupJoinRequest;
import com.example.backend.group.entity.GroupJoinRequestStatus;
import com.example.backend.group.entity.GroupMember;
import com.example.backend.group.entity.GroupMessage;
import com.example.backend.group.entity.GroupMessageHidden;
import com.example.backend.group.entity.PinnedGroupMessage;
import com.example.backend.group.repository.GroupJoinRequestRepository;
import com.example.backend.group.repository.GroupMemberRepository;
import com.example.backend.group.repository.GroupMessageHiddenRepository;
import com.example.backend.group.repository.GroupMessageReactionRepository;
import com.example.backend.group.repository.GroupMessageRepository;
import com.example.backend.group.repository.GroupRepository;
import com.example.backend.group.repository.PinnedGroupMessageRepository;
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
    private final PinnedGroupMessageRepository pinnedGroupMessageRepository;
    private final GroupMessageHiddenRepository groupMessageHiddenRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
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

        saved.getMembers().add(GroupMember.of(saved, creator, true));

        for (UUID memberId : request.getMemberIds()) {
            if (memberId.equals(creator.getId())) continue;
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
            saved.getMembers().add(GroupMember.of(saved, member, false));
        }

        groupRepository.save(saved);
        log.info("Group '{}' created by {}", saved.getName(), creator.getEmail());
        return toGroupDto(saved, creator.getId(), false);
    }

    // ─── Lấy danh sách nhóm của user ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupDto> getMyGroups(Authentication currentUser) {
        User user = getUser(currentUser);
        return groupRepository.findAllGroupsByUserId(user.getId())
                .stream()
                .map(g -> toGroupDto(g, user.getId(), false))
                .collect(Collectors.toList());
    }

    // ─── Lấy chi tiết nhóm ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GroupDto getGroupById(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());
        return toGroupDto(group, user.getId(), true);
    }

    // ─── Cập nhật thông tin nhóm (chỉ admin) ─────────────────────────────────

    @Transactional
    public GroupDto updateGroup(UUID groupId, GroupRequest.Update request, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        boolean nameChanged = request.getName() != null && !request.getName().equals(group.getName());
        String newName = request.getName();

        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        if (request.getAvatarUrl() != null) group.setAvatarUrl(request.getAvatarUrl());

        groupRepository.save(group);
        log.info("Group '{}' updated by {}", group.getName(), user.getEmail());

        GroupDto dto = toGroupDto(group, user.getId(), false);
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.GROUP_UPDATED, groupId, null, user.getId(), null, dto, null, null));

        if (nameChanged) {
            String actorName = user.getFirstName() + " " + user.getLastName();
            sendSystemMessage(groupId, group, user,
                    actorName + " đã đổi tên nhóm thành \"" + newName + "\"");
        }
        return dto;
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

        GroupDto dto = toGroupDto(group, user.getId(), false);
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.GROUP_UPDATED, groupId, null, user.getId(), null, dto, null, null));

        String actorName = user.getFirstName() + " " + user.getLastName();
        sendSystemMessage(groupId, group, user, actorName + " đã thay đổi ảnh đại diện nhóm");
        return dto;
    }

    // ─── Thêm thành viên (chỉ admin) ─────────────────────────────────────────

    @Transactional
    public GroupDto addMembers(UUID groupId, GroupRequest.AddMember request, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        List<String> addedNames = new java.util.ArrayList<>();
        for (UUID userId : request.getUserIds()) {
            if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) continue;
            User newMember = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            group.getMembers().add(GroupMember.of(group, newMember, false));
            addedNames.add(newMember.getFirstName() + " " + newMember.getLastName());
        }

        groupRepository.save(group);
        log.info("{} members added to group '{}'", request.getUserIds().size(), group.getName());

        GroupDto dto = toGroupDto(group, user.getId(), false);
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.MEMBER_ADDED, groupId, null, user.getId(), null, dto, null, null));

        if (!addedNames.isEmpty()) {
            String actorName = user.getFirstName() + " " + user.getLastName();
            sendSystemMessage(groupId, group, user,
                    actorName + " đã thêm " + String.join(", ", addedNames) + " vào nhóm");
        }
        return dto;
    }

    // ─── Xóa thành viên (chỉ admin, không thể xóa chính mình) ───────────────

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        if (user.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Admin không thể tự xóa mình, hãy dùng rời nhóm");
        }

        // Phải remove khỏi collection thay vì gọi repo.delete() trực tiếp.
        // Group.members có orphanRemoval=true: nếu delete() trực tiếp mà entity vẫn còn
        // trong collection thì Hibernate conflict → re-insert hoặc không delete được.
        GroupMember member = group.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(targetUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));

        String kickedName = member.getUser().getFirstName() + " " + member.getUser().getLastName();
        String kickedEmail = member.getUser().getEmail();
        group.getMembers().remove(member);
        groupRepository.save(group);
        log.info("User {} removed from group {}", targetUserId, groupId);

        // Broadcast đến tất cả thành viên đang xem nhóm
        GroupEventDto event = new GroupEventDto(
                GroupEventDto.MEMBER_REMOVED, groupId, targetUserId, user.getId(), null, null, null, null);
        broadcastGroupEvent(groupId, event);

        // Gửi personal event cho người bị kick (để remove group khỏi list)
        messagingTemplate.convertAndSendToUser(kickedEmail, "/queue/group-events", event);

        String actorName = user.getFirstName() + " " + user.getLastName();
        sendSystemMessage(groupId, group, user, actorName + " đã xóa " + kickedName + " khỏi nhóm");
    }

    // ─── Rời nhóm ────────────────────────────────────────────────────────────

    @Transactional
    public void leaveGroup(UUID groupId, UUID newAdminId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, user.getId());

        boolean isAdmin = group.isAdmin(user.getId());
        long adminCount = group.getMembers().stream().filter(GroupMember::isAdmin).count();
        long memberCount = group.getMembers().size();

        // Admin cuối cùng rời nhóm khi còn thành viên khác → phải chỉ định admin mới
        if (isAdmin && adminCount == 1 && memberCount > 1) {
            if (newAdminId == null) {
                throw new IllegalArgumentException("Bạn là admin cuối cùng, hãy chỉ định admin mới trước khi rời nhóm");
            }
            GroupMember newAdmin = groupMemberRepository
                    .findByGroupIdAndUserId(groupId, newAdminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong nhóm"));
            if (newAdmin.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Không thể chọn chính mình làm admin mới");
            }
            newAdmin.setAdmin(true);
            groupMemberRepository.save(newAdmin);
        }

        GroupMember member = group.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải thành viên nhóm này"));

        group.getMembers().remove(member);
        groupRepository.save(group);

        if (groupMemberRepository.countByGroupId(groupId) == 0) {
            groupRepository.delete(group);
            log.info("Group {} deleted — no members left", groupId);
        } else {
            log.info("User {} left group {}", user.getEmail(), groupId);
            broadcastGroupEvent(groupId, new GroupEventDto(
                    GroupEventDto.MEMBER_LEFT, groupId, user.getId(), user.getId(), null, null, null, null));
            String actorName = user.getFirstName() + " " + user.getLastName();
            sendSystemMessage(groupId, group, user, actorName + " đã rời nhóm");
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

        // Luôn transfer: người gọi mất quyền admin, người được chỉ định trở thành admin mới
        GroupMember callerMember = groupMemberRepository
                .findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));
        callerMember.setAdmin(false);
        groupMemberRepository.save(callerMember);

        member.setAdmin(true);
        groupMemberRepository.save(member);
        log.info("User {} transferred admin to {} in group {}", user.getEmail(), targetUserId, groupId);

        GroupDto dto = toGroupDto(group, user.getId(), false);
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.ADMIN_CHANGED, groupId, targetUserId, user.getId(), null, dto, null, null));

        String actorName = user.getFirstName() + " " + user.getLastName();
        String targetName = member.getUser().getFirstName() + " " + member.getUser().getLastName();
        sendSystemMessage(groupId, group, user,
                actorName + " đã nhường quyền quản trị viên cho " + targetName);
        return dto;
    }

    // ─── Giải tán nhóm (chỉ admin) ────────────────────────────────────────────

    @Transactional
    public void dissolveGroup(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, user.getId());

        // Collect member emails BEFORE deleting so we can send personal notifications
        List<String> memberEmails = group.getMembers().stream()
                .map(m -> m.getUser().getEmail())
                .filter(email -> !email.equals(user.getEmail()))
                .toList();

        GroupEventDto event = new GroupEventDto(
                GroupEventDto.GROUP_DISSOLVED, groupId, null, user.getId(), null, null, null, null);

        // Broadcast to group topic (for active GroupWindow viewers)
        broadcastGroupEvent(groupId, event);

        // Send personal event to each member for sidebar removal
        memberEmails.forEach(email ->
                messagingTemplate.convertAndSendToUser(email, "/queue/group-events", event));

        groupMemberRepository.deleteByGroupId(groupId);
        groupRepository.delete(group);
        log.info("Group {} dissolved by admin {}", groupId, user.getEmail());
    }

    // ─── Ghim tin nhắn (mọi thành viên) ──────────────────────────────────────

    @Transactional
    public List<GroupMessageDto> pinMessage(UUID groupId, UUID messageId, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckMember(groupId, user.getId());

        GroupMessage msg = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn không tồn tại"));

        if (!msg.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Tin nhắn không thuộc nhóm này");
        }

        if (pinnedGroupMessageRepository.findByGroupIdAndMessageId(groupId, messageId).isPresent()) {
            throw new IllegalArgumentException("Tin nhắn đã được ghim");
        }

        long count = pinnedGroupMessageRepository.countByGroupId(groupId);
        if (count >= 3) {
            throw new IllegalArgumentException("Nhóm chỉ được ghim tối đa 3 tin nhắn");
        }

        PinnedGroupMessage pinned = new PinnedGroupMessage();
        pinned.setGroup(msg.getGroup());
        pinned.setMessage(msg);
        pinned.setPinnedBy(user);
        pinnedGroupMessageRepository.save(pinned);

        List<GroupMessageDto> pinnedDtos = getPinnedMessageDtos(groupId, user.getId());
        String actorName = user.getFirstName() + " " + user.getLastName();
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.MESSAGE_PINNED, groupId, null, user.getId(), actorName, null, pinnedDtos, null));
        return pinnedDtos;
    }

    // ─── Bỏ ghim tin nhắn (mọi thành viên) ──────────────────────────────────

    @Transactional
    public List<GroupMessageDto> unpinMessage(UUID groupId, UUID messageId, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckMember(groupId, user.getId());

        pinnedGroupMessageRepository.deleteByGroupIdAndMessageId(groupId, messageId);

        List<GroupMessageDto> pinnedDtos = getPinnedMessageDtos(groupId, user.getId());
        String actorName = user.getFirstName() + " " + user.getLastName();
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.MESSAGE_UNPINNED, groupId, null, user.getId(), actorName, null, pinnedDtos, null));
        return pinnedDtos;
    }

    // ─── Lấy danh sách tin nhắn đã ghim ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupMessageDto> getPinnedMessages(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckMember(groupId, user.getId());
        return getPinnedMessageDtos(groupId, user.getId());
    }

    // ─── Yêu cầu tham gia nhóm (mọi thành viên tạo, admin duyệt) ────────────

    @Transactional
    public List<GroupJoinRequestDto> createJoinRequests(UUID groupId, List<UUID> userIds, Authentication currentUser) {
        User requester = getUser(currentUser);
        Group group = getGroupAndCheckMember(groupId, requester.getId());

        List<GroupJoinRequest> created = new java.util.ArrayList<>();
        for (UUID targetId : userIds) {
            if (groupMemberRepository.existsByGroupIdAndUserId(groupId, targetId)) continue;
            if (groupJoinRequestRepository
                    .findByGroupIdAndTargetUserIdAndStatus(groupId, targetId, GroupJoinRequestStatus.PENDING)
                    .isPresent()) continue;
            User target = userRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + targetId));
            GroupJoinRequest req = new GroupJoinRequest();
            req.setGroup(group);
            req.setRequestedBy(requester);
            req.setTargetUser(target);
            groupJoinRequestRepository.save(req);
            created.add(req);
        }

        List<GroupJoinRequestDto> dtos = created.stream().map(this::toJoinRequestDto).toList();

        // Notify all admins via personal queue
        String requesterName = requester.getFirstName() + " " + requester.getLastName();
        group.getMembers().stream()
                .filter(GroupMember::isAdmin)
                .forEach(admin -> dtos.forEach(dto ->
                    messagingTemplate.convertAndSendToUser(
                        admin.getUser().getEmail(), "/queue/group-events",
                        new GroupEventDto(GroupEventDto.JOIN_REQUEST, groupId, null,
                                requester.getId(), requesterName, null, null, dto)
                    )
                ));

        return dtos;
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestDto> getJoinRequests(UUID groupId, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckAdmin(groupId, user.getId());
        return groupJoinRequestRepository
                .findByGroupIdAndStatus(groupId, GroupJoinRequestStatus.PENDING)
                .stream().map(this::toJoinRequestDto).toList();
    }

    @Transactional
    public GroupDto approveJoinRequest(UUID groupId, UUID requestId, Authentication currentUser) {
        User admin = getUser(currentUser);
        Group group = getGroupAndCheckAdmin(groupId, admin.getId());

        GroupJoinRequest req = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        if (req.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu đã được xử lý");
        }

        User target = req.getTargetUser();
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, target.getId())) {
            group.getMembers().add(GroupMember.of(group, target, false));
            groupRepository.save(group);
        }

        req.setStatus(GroupJoinRequestStatus.APPROVED);
        groupJoinRequestRepository.save(req);

        GroupDto dto = toGroupDto(group, admin.getId(), false);
        broadcastGroupEvent(groupId, new GroupEventDto(
                GroupEventDto.MEMBER_ADDED, groupId, null, admin.getId(), null, dto, null, null));
        return dto;
    }

    @Transactional
    public void rejectJoinRequest(UUID groupId, UUID requestId, Authentication currentUser) {
        User admin = getUser(currentUser);
        getGroupAndCheckAdmin(groupId, admin.getId());

        GroupJoinRequest req = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        if (req.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu đã được xử lý");
        }
        req.setStatus(GroupJoinRequestStatus.REJECTED);
        groupJoinRequestRepository.save(req);
    }

    private GroupJoinRequestDto toJoinRequestDto(GroupJoinRequest req) {
        User by = req.getRequestedBy();
        User target = req.getTargetUser();
        return new GroupJoinRequestDto(
                req.getId(),
                by.getId(),
                by.getFirstName() + " " + by.getLastName(),
                by.getAvatarUrl(),
                target.getId(),
                target.getFirstName() + " " + target.getLastName(),
                target.getAvatarUrl(),
                req.getCreatedDate() != null ? req.getCreatedDate().toString() : null
        );
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
        GroupMessageDto dto = toMessageDto(saved, user.getId(), Set.of());

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
        GroupMessageDto dto = toMessageDto(saved, user.getId(), Set.of());

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

        GroupMessageDto dto = toMessageDto(msg, user.getId(), Set.of());
        messagingTemplate.convertAndSend("/topic/group/" + msg.getGroup().getId(), dto);
        log.info("Group message {} recalled by {}", messageId, user.getEmail());
    }

    // ─── Xóa tin nhắn phía mình ──────────────────────────────────────────────

    @Transactional
    public void deleteGroupMessageForMe(UUID messageId, Authentication currentUser) {
        User user = getUser(currentUser);

        GroupMessage msg = groupMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tin nhắn không tồn tại: " + messageId));

        getGroupAndCheckMember(msg.getGroup().getId(), user.getId());

        if (groupMessageHiddenRepository.existsByMessageIdAndUserId(messageId, user.getId())) {
            return; // Already hidden, idempotent
        }

        GroupMessageHidden hidden = new GroupMessageHidden(msg, user);
        groupMessageHiddenRepository.save(hidden);
        log.info("Group message {} hidden for user {}", messageId, user.getEmail());
    }

    // ─── Lấy tin nhắn nhóm (phân trang) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupMessageDto> getMessages(UUID groupId, int page, int size, Authentication currentUser) {
        User user = getUser(currentUser);
        getGroupAndCheckMember(groupId, user.getId());

        List<GroupMessage> messages = groupMessageRepository
                .findByGroupIdAndDeletedFalseOrderByCreatedDateAsc(groupId, PageRequest.of(page, size))
                .getContent();

        List<UUID> msgIds = messages.stream().map(GroupMessage::getId).collect(Collectors.toList());

        // Lấy pinned IDs
        Set<UUID> pinnedIds = pinnedGroupMessageRepository.findByGroupIdOrderByCreatedDateDesc(groupId)
                .stream().map(p -> p.getMessage().getId()).collect(Collectors.toSet());

        // Lấy hidden IDs cho user hiện tại
        Set<UUID> hiddenIds = msgIds.isEmpty() ? Set.of()
                : groupMessageHiddenRepository.findHiddenMessageIds(msgIds, user.getId());

        List<GroupMessageDto> dtos = messages.stream()
                .filter(m -> !hiddenIds.contains(m.getId()))
                .map(m -> toMessageDto(m, user.getId(), pinnedIds))
                .collect(Collectors.toList());

        // Gắn reactions (batch query)
        List<UUID> visibleMsgIds = dtos.stream().map(GroupMessageDto::getId).collect(Collectors.toList());
        if (!visibleMsgIds.isEmpty()) {
            Map<UUID, List<ReactionDto>> reactionsByMsgId = groupMessageReactionRepository
                    .findByGroupMessageIdIn(visibleMsgIds)
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

    private void broadcastGroupEvent(UUID groupId, GroupEventDto event) {
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/events", event);
    }

    private void sendSystemMessage(UUID groupId, Group group, User actor, String content) {
        GroupMessage msg = new GroupMessage();
        msg.setGroup(group);
        msg.setSender(actor);
        msg.setContent(content);
        msg.setType(MessageType.SYSTEM);
        GroupMessage saved = groupMessageRepository.save(msg);
        GroupMessageDto dto = toMessageDto(saved, actor.getId(), Set.of());
        messagingTemplate.convertAndSend("/topic/group/" + groupId, dto);
    }

    private List<GroupMessageDto> getPinnedMessageDtos(UUID groupId, UUID currentUserId) {
        return pinnedGroupMessageRepository.findByGroupIdOrderByCreatedDateDesc(groupId)
                .stream()
                .map(p -> toMessageDto(p.getMessage(), currentUserId, Set.of(p.getMessage().getId())))
                .collect(Collectors.toList());
    }

    private GroupDto toGroupDto(Group group, UUID currentUserId, boolean includePinned) {
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

        // Preview tin nhắn cuối
        groupMessageRepository.findTop1ByGroupIdOrderByCreatedDateDesc(group.getId())
                .ifPresent(last -> {
                    dto.setLastMessage(last.getContent());
                    dto.setLastMessageType(last.getType());
                    dto.setLastMessageTime(last.getCreatedDate());
                    dto.setLastMessageSenderName(
                            last.getSender().getFirstName() + " " + last.getSender().getLastName()
                    );
                });

        if (includePinned) {
            dto.setPinnedMessages(getPinnedMessageDtos(group.getId(), currentUserId));
        }

        return dto;
    }

    private GroupMessageDto toMessageDto(GroupMessage msg, UUID currentUserId, Set<UUID> pinnedIds) {
        boolean isMedia = msg.getType() != null
                && msg.getType() != MessageType.TEXT
                && msg.getType() != MessageType.SYSTEM;
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
                .pinned(pinnedIds != null && pinnedIds.contains(msg.getId()))
                .build();
    }
}
