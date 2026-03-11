package com.example.backend.mappers;

import com.example.backend.Entities.Message;
import com.example.backend.enums.MessageType;
import com.example.backend.models.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "createdAt", source = "createdDate")

    @Mapping(target = "receiverId", expression = "java(message.getReceiver().getId())")

    @Mapping(target = "mediaUrl", source = "message", qualifiedByName = "toMediaUrl")
    MessageDto toDto(Message message);

    @Named("toMediaUrl")
    default String toMediaUrl(Message message) {
        if (message.getType() == null || message.getType() == MessageType.TEXT) {
            return null;
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            return null;
        }
        return "/api/v1/message/media/" + message.getContent();
    }
}