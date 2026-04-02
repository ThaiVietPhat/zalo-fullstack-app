package com.example.backend.mappers;

import com.example.backend.Entities.User;
import com.example.backend.models.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "online", expression = "java(user.isUserOnline())")
    @Mapping(target = "lastSeenText", expression = "java(user.getLastSeenText())")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "banned", source = "banned")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    UserDto toDto(User user);
}
