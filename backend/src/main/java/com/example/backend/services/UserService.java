package com.example.backend.services;

import com.example.backend.models.ChangePasswordRequest;
import com.example.backend.models.UpdateProfileRequest;
import com.example.backend.models.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsersExceptSelf(Authentication currentUser);

    UserDto getMyProfile(Authentication currentUser);

    List<UserDto> searchUsers(String keyword, Authentication currentUser);

    UserDto updateProfile(UpdateProfileRequest request, Authentication currentUser);

    UserDto uploadAvatar(MultipartFile file, Authentication currentUser);

    void changePassword(ChangePasswordRequest request, Authentication currentUser);
}