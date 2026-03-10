package com.example.backend.services;

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
        UUID userId = UUID.fromString(currentUser.getName());

        return userRepository.findByIdNot(userId)
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }
}