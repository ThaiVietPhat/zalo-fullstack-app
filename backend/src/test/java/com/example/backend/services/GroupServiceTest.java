package com.example.backend.services;

import com.example.backend.Entities.Group;
import com.example.backend.Entities.GroupMember;
import com.example.backend.Entities.GroupMessage;
import com.example.backend.Entities.User;
import com.example.backend.enums.MessageType;
import com.example.backend.models.GroupDto;
import com.example.backend.models.GroupMessageDto;
import com.example.backend.models.GroupRequest;
import com.example.backend.repositories.GroupMemberRepository;
import com.example.backend.repositories.GroupMessageReactionRepository;
import com.example.backend.repositories.GroupMessageRepository;
import com.example.backend.repositories.GroupRepository;
import com.example.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService Unit Tests")
class GroupServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock GroupMessageRepository groupMessageRepository;
    @Mock GroupMessageReactionRepository groupMessageReactionRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock FileStorageService fileStorageService;
    @Mock Authentication authentication;

    @InjectMocks GroupService groupService;

    private User creator;
    private User member1;
    private User member2;
    private Group group;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();

        creator = new User();
        creator.setId(UUID.randomUUID());
        creator.setEmail("creator@gmail.com");
        creator.setFirstName("Creator");
        creator.setLastName("User");

        member1 = new User();
        member1.setId(UUID.randomUUID());
        member1.setEmail("member1@gmail.com");
        member1.setFirstName("Member");
        member1.setLastName("One");

        member2 = new User();
        member2.setId(UUID.randomUUID());
        member2.setEmail("member2@gmail.com");
        member2.setFirstName("Member");
        member2.setLastName("Two");

        group = new Group();
        group.setId(groupId);
        group.setName("Test Group");
        group.setCreatedBy(creator);
        group.setMembers(new ArrayList<>(List.of(
                GroupMember.of(group, creator, true),
                GroupMember.of(group, member1, false)
        )));

        when(authentication.getName()).thenReturn("creator@gmail.com");
    }

    // ─── createGroup ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Tạo nhóm thành công")
    void createGroup_success() {
        GroupRequest.Create req = GroupRequest.Create.builder()
                .name("Nhóm mới")
                .description("Mô tả")
                .memberIds(List.of(member1.getId(), member2.getId()))
                .build();

        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
        when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
        when(groupRepository.save(any())).thenReturn(group);
        when(groupMessageRepository.findTop1ByGroupIdOrderByCreatedDateDesc(any()))
                .thenReturn(Optional.empty());

        GroupDto result = groupService.createGroup(req, authentication);

        assertThat(result).isNotNull();
        verify(groupRepository, times(2)).save(any(Group.class));
    }

    // ─── getMyGroups ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy danh sách nhóm của user")
    void getMyGroups_success() {
        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(groupRepository.findAllGroupsByUserId(creator.getId())).thenReturn(List.of(group));
        when(groupMessageRepository.findTop1ByGroupIdOrderByCreatedDateDesc(any()))
                .thenReturn(Optional.empty());

        List<GroupDto> result = groupService.getMyGroups(authentication);

        assertThat(result).hasSize(1);
    }

    // ─── addMembers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Thêm thành viên thành công - admin")
    void addMembers_success() {
        GroupRequest.AddMember req = new GroupRequest.AddMember(List.of(member2.getId()));

        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, member2.getId())).thenReturn(false);
        when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
        when(groupRepository.save(any())).thenReturn(group);
        when(groupMessageRepository.findTop1ByGroupIdOrderByCreatedDateDesc(any()))
                .thenReturn(Optional.empty());

        GroupDto result = groupService.addMembers(groupId, req, authentication);
        assertThat(result).isNotNull();
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("Thêm thành viên thất bại - không phải admin")
    void addMembers_notAdmin_fails() {
        when(authentication.getName()).thenReturn("member1@gmail.com");
        when(userRepository.findByEmail("member1@gmail.com")).thenReturn(Optional.of(member1));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        GroupRequest.AddMember req = new GroupRequest.AddMember(List.of(member2.getId()));

        assertThatThrownBy(() -> groupService.addMembers(groupId, req, authentication))
                .isInstanceOf(com.example.backend.exceptions.UnauthorizedException.class)
                .hasMessageContaining("admin");
    }

    // ─── removeMember ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Xóa thành viên thành công - admin")
    void removeMember_success() {
        GroupMember memberToRemove = GroupMember.of(group, member1, false);

        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, member1.getId()))
                .thenReturn(Optional.of(memberToRemove));

        assertThatNoException().isThrownBy(
                () -> groupService.removeMember(groupId, member1.getId(), authentication));
        verify(groupMemberRepository).delete(memberToRemove);
    }

    @Test
    @DisplayName("Admin không thể tự xóa mình")
    void removeMember_adminRemoveSelf_fails() {
        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(
                () -> groupService.removeMember(groupId, creator.getId(), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rời nhóm");
    }

    // ─── sendMessage ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gửi tin nhắn nhóm thành công")
    void sendMessage_success() {
        GroupRequest.SendMessage req = new GroupRequest.SendMessage("Hello nhóm!", MessageType.TEXT);

        GroupMessage savedMsg = new GroupMessage();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setContent("Hello nhóm!");
        savedMsg.setType(MessageType.TEXT);
        savedMsg.setGroup(group);
        savedMsg.setSender(creator);

        when(userRepository.findByEmail("creator@gmail.com")).thenReturn(Optional.of(creator));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMessageRepository.save(any())).thenReturn(savedMsg);

        GroupMessageDto result = groupService.sendMessage(groupId, req, authentication);

        assertThat(result.getContent()).isEqualTo("Hello nhóm!");
        assertThat(result.isMine()).isTrue();
        verify(messagingTemplate).convertAndSend(eq("/topic/group/" + groupId), any(GroupMessageDto.class));
    }

    @Test
    @DisplayName("Gửi tin nhắn thất bại - không phải thành viên")
    void sendMessage_notMember_fails() {
        User stranger = new User();
        stranger.setId(UUID.randomUUID());
        stranger.setEmail("stranger@gmail.com");

        when(authentication.getName()).thenReturn("stranger@gmail.com");
        when(userRepository.findByEmail("stranger@gmail.com")).thenReturn(Optional.of(stranger));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        GroupRequest.SendMessage req = new GroupRequest.SendMessage("Hello", MessageType.TEXT);

        assertThatThrownBy(() -> groupService.sendMessage(groupId, req, authentication))
                .isInstanceOf(com.example.backend.exceptions.UnauthorizedException.class)
                .hasMessageContaining("thành viên");
    }
}