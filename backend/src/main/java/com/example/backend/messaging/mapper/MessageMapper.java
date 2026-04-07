package com.example.backend.messaging.mapper;

import com.example.backend.messaging.entity.Message;
import com.example.backend.messaging.enums.MessageType;
import com.example.backend.messaging.dto.MessageDto;
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
    @Mapping(target = "reactions", ignore = true)
    MessageDto toDto(Message message);

    @Named("toMediaUrl")
    default String toMediaUrl(Message message) {
        if (message.getType() == null || message.getType() == MessageType.TEXT) {
            return null;
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            return null;
        }
        String content = message.getContent();
        // Nếu đã là S3 URL đầy đủ thì dùng luôn
        if (content.startsWith("http")) {
            return content;
        }
        return "/api/v1/message/media/" + content;
    }
}