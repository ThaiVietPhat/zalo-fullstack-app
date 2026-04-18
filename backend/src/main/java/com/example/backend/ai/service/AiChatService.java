package com.example.backend.ai.service;

import com.example.backend.ai.entity.AiMessage;
import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.ai.dto.AiMessageDto;
import com.example.backend.ai.repository.AiMessageRepository;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý AI thông minh tích hợp trong ứng dụng chat Zalo Clone.
            Hãy trả lời bằng tiếng Việt khi người dùng viết tiếng Việt,
            và bằng tiếng Anh khi họ viết tiếng Anh.
            Câu trả lời nên ngắn gọn, rõ ràng và hữu ích.
            """;

    private final ChatClient chatClient;
    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         AiMessageRepository aiMessageRepository,
                         UserRepository userRepository) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.aiMessageRepository = aiMessageRepository;
        this.userRepository = userRepository;
    }

    // ─── Gửi tin nhắn tới AI ─────────────────────────────────────────────────

    @Transactional
    public AiMessageDto sendMessage(AiChatRequest request, Authentication auth) {
        User user = getUser(auth);

        // Lưu tin nhắn của user
        AiMessage userMsg = new AiMessage();
        userMsg.setUser(user);
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        aiMessageRepository.save(userMsg);

        // Lấy lịch sử hội thoại (newest-first → reverse thành oldest-first)
        List<AiMessage> history = aiMessageRepository
                .findTop20ByUserIdOrderByCreatedDateDesc(user.getId());
        Collections.reverse(history);

        // Gọi AI
        String assistantReply = callAi(history);

        // Lưu phản hồi của AI
        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setUser(user);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantReply);
        AiMessage saved = aiMessageRepository.save(assistantMsg);

        return toDto(saved);
    }

    // ─── Lấy lịch sử hội thoại ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AiMessageDto> getHistory(int page, int size, Authentication auth) {
        User user = getUser(auth);
        return aiMessageRepository
                .findByUserIdOrderByCreatedDateAsc(user.getId(), PageRequest.of(page, size))
                .map(this::toDto);
    }

    // ─── Xóa lịch sử ─────────────────────────────────────────────────────────

    @Transactional
    public void clearHistory(Authentication auth) {
        User user = getUser(auth);
        aiMessageRepository.deleteByUserId(user.getId());
        log.info("AI chat history cleared for user {}", user.getId());
    }

    // ─── Gọi AI qua Spring AI ChatClient ─────────────────────────────────────

    private String callAi(List<AiMessage> history) {
        List<Message> messages = history.stream()
                .map(m -> "user".equals(m.getRole())
                        ? (Message) new UserMessage(m.getContent())
                        : (Message) new AssistantMessage(m.getContent()))
                .collect(Collectors.toList());

        try {
            return chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("429") || msg.toLowerCase().contains("rate limit")) {
                log.warn("AI rate limit hit: {}", msg);
                return "Trợ lý AI đang bận, vui lòng thử lại sau vài giây.";
            }
            log.error("AI call failed: {} — {}", e.getClass().getSimpleName(), msg);
            return "Xin lỗi, đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AiMessageDto toDto(AiMessage msg) {
        return AiMessageDto.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .createdDate(msg.getCreatedDate())
                .build();
    }
}
