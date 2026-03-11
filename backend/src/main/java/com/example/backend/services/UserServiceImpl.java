package com.example.backend.services;

import com.example.backend.Entities.User;
import com.example.backend.mappers.UserMapper;
import com.example.backend.models.UserDto;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getAllUsersExceptSelf(Authentication currentUser) {
        String email = currentUser.getName();

        User self = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userRepository.findByIdNot(self.getId())
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getMyProfile(Authentication currentUser) {
        String email = currentUser.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> searchUsers(String keyword, Authentication currentUser) {
        String email = currentUser.getName();
        User self = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userRepository.searchByNameOrEmail(keyword, self.getId())
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}