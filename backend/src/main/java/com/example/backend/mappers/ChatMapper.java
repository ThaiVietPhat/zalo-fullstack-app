package com.example.backend.mappers;

import com.example.backend.Entities.Chat;
import com.example.backend.models.ChatDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "user1Id", source = "user1.id")
    @Mapping(target = "user2Id", source = "user2.id")

    @Mapping(target = "chatName", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "lastMessageType", ignore = true)
    @Mapping(target = "lastMessageTime", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "recipientOnline", ignore = true)

    @Mapping(target = "recipientLastSeenText", ignore = true)

    ChatDto toDto(Chat chat);
}