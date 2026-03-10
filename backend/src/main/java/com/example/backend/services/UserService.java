package com.example.backend.services;

import com.example.backend.models.UserDto;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsersExceptSelf(Authentication currentUser);

}
