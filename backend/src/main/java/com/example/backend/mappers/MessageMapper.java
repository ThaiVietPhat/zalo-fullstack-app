package com.example.backend.mappers;

import com.example.backend.Entities.Message;
import com.example.backend.models.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "createdAt", source = "createdDate")
    MessageDto toDto(Message message);
}