package com.example.backend.call.service;

import com.example.backend.call.dto.CallSessionDto;
import com.example.backend.call.dto.StartCallRequest;
import com.example.backend.call.entity.CallSession;
import com.example.backend.call.entity.CallSession.CallStatus;
import com.example.backend.call.entity.CallSession.CallType;
import com.example.backend.call.repository.CallSessionRepository;
import com.example.backend.chat.entity.Chat;
import com.example.backend.chat.repository.ChatRepository;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallHistoryService {

    private final CallSessionRepository callSessionRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    /**
     * Lưu lịch sử cuộc gọi sau khi kết thúc.
     * Chỉ caller (initiator) gọi endpoint này.
     */
    @Transactional
    public CallSessionDto saveCallSession(UUID initiatorId, StartCallRequest req) {
        Chat chat = chatRepository.findById(req.getChatId())
            .orElseThrow(() -> new RuntimeException("Chat not found: " + req.getChatId()));

        User initiator = userRepository.findById(initiatorId)
            .orElseThrow(() -> new RuntimeException("User not found: " + initiatorId));

        User receiver = userRepository.findById(req.getReceiverId())
            .orElseThrow(() -> new RuntimeException("User not found: " + req.getReceiverId()));

        CallSession session = CallSession.builder()
            .chat(chat)
            .initiator(initiator)
            .receiver(receiver)
            .callType(CallType.valueOf(req.getCallType()))
            .status(CallStatus.valueOf(req.getStatus()))
            .durationSec(req.getDurationSec())
            .startedAt(LocalDateTime.now())
            .endedAt(req.getDurationSec() != null ? LocalDateTime.now() : null)
            .build();

        CallSession saved = callSessionRepository.save(session);
        return toDto(saved);
    }

    /**
     * Lấy lịch sử cuộc gọi của một chat.
     */
    @Transactional(readOnly = true)
    public List<CallSessionDto> getCallHistory(UUID chatId) {
        return callSessionRepository
            .findByChatIdOrderByStartedAtDesc(chatId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    // ------------------------------------------------------------------ //

    private CallSessionDto toDto(CallSession cs) {
        return CallSessionDto.builder()
            .id(cs.getId())
            .chatId(cs.getChat().getId())
            .initiatorId(cs.getInitiator().getId())
            .initiatorName(cs.getInitiator().getFirstName() + " " + cs.getInitiator().getLastName())
            .initiatorAvatar(cs.getInitiator().getAvatarUrl())
            .receiverId(cs.getReceiver().getId())
            .receiverName(cs.getReceiver().getFirstName() + " " + cs.getReceiver().getLastName())
            .receiverAvatar(cs.getReceiver().getAvatarUrl())
            .callType(cs.getCallType())
            .status(cs.getStatus())
            .durationSec(cs.getDurationSec())
            .startedAt(cs.getStartedAt())
            .endedAt(cs.getEndedAt())
            .build();
    }
}
