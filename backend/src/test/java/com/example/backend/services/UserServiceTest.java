package com.example.backend.services;

import com.example.backend.user.entity.User;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.dto.UpdateProfileRequest;
import com.example.backend.user.dto.UserDto;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock Authentication authentication;

    @InjectMocks UserServiceImpl userService;

    private User self;
    private User other1;
    private User other2;
    private UserDto selfDto;
    private UserDto otherDto1;

    @BeforeEach
    void setUp() {
        self = new User();
        self.setId(UUID.randomUUID());
        self.setEmail("self@gmail.com");
        self.setFirstName("Self");
        self.setLastName("User");

        other1 = new User();
        other1.setId(UUID.randomUUID());
        other1.setEmail("other1@gmail.com");
        other1.setFirstName("Other");
        other1.setLastName("One");

        other2 = new User();
        other2.setId(UUID.randomUUID());
        other2.setEmail("other2@gmail.com");
        other2.setFirstName("Other");
        other2.setLastName("Two");

        selfDto  = new UserDto();
        otherDto1 = new UserDto();

        when(authentication.getName()).thenReturn("self@gmail.com");
    }

    // ─── getAllUsersExceptSelf ─────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy danh sách user trừ bản thân")
    void getAllUsersExceptSelf_success() {
        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.of(self));
        when(userRepository.findByIdNot(self.getId())).thenReturn(List.of(other1, other2));
        when(userMapper.toDto(any())).thenReturn(otherDto1);

        List<UserDto> result = userService.getAllUsersExceptSelf(authentication);

        assertThat(result).hasSize(2);
        verify(userRepository).findByIdNot(self.getId());
    }

    @Test
    @DisplayName("Lấy danh sách user - user không tồn tại")
    void getAllUsersExceptSelf_userNotFound() {
        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAllUsersExceptSelf(authentication))
                .isInstanceOf(com.example.backend.shared.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ─── getMyProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy profile bản thân thành công")
    void getMyProfile_success() {
        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.of(self));
        when(userMapper.toDto(self)).thenReturn(selfDto);

        UserDto result = userService.getMyProfile(authentication);

        assertThat(result).isEqualTo(selfDto);
    }

    // ─── searchUsers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Tìm kiếm user theo keyword")
    void searchUsers_success() {
        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.of(self));
        when(userRepository.searchByNameOrEmail("other", self.getId()))
                .thenReturn(List.of(other1, other2));
        when(userMapper.toDto(any())).thenReturn(otherDto1);

        List<UserDto> result = userService.searchUsers("other", authentication);

        assertThat(result).hasSize(2);
        verify(userRepository).searchByNameOrEmail("other", self.getId());
    }

    @Test
    @DisplayName("Tìm kiếm user - không có kết quả")
    void searchUsers_noResult() {
        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.of(self));
        when(userRepository.searchByNameOrEmail("xyz", self.getId())).thenReturn(List.of());

        List<UserDto> result = userService.searchUsers("xyz", authentication);

        assertThat(result).isEmpty();
    }

    // ─── updateProfile ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Cập nhật profile thành công")
    void updateProfile_success() {
        UpdateProfileRequest req = new UpdateProfileRequest("Nguyen", "Van A");

        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.of(self));
        when(userRepository.save(any())).thenReturn(self);
        when(userMapper.toDto(self)).thenReturn(selfDto);

        UserDto result = userService.updateProfile(req, authentication);

        assertThat(result).isEqualTo(selfDto);
        assertThat(self.getFirstName()).isEqualTo("Nguyen");
        assertThat(self.getLastName()).isEqualTo("Van A");
        verify(userRepository).save(self);
    }

    @Test
    @DisplayName("Cập nhật profile - user không tồn tại")
    void updateProfile_userNotFound() {
        UpdateProfileRequest req = new UpdateProfileRequest("New Name", null);

        when(userRepository.findByEmail("self@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(req, authentication))
                .isInstanceOf(com.example.backend.shared.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}