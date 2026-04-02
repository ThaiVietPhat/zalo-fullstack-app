package com.example.backend.services;

import com.example.backend.Entities.AiMessage;
import com.example.backend.Entities.User;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.models.AiChatRequest;
import com.example.backend.models.AiMessageDto;
import com.example.backend.repositories.AiMessageRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final WebClient aiWebClient;

    @Value("${app.claude.api-key:}")
    private String apiKey;

    @Value("${app.claude.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${app.claude.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.claude.context-turns:20}")
    private int contextTurns;

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

        // Lấy lịch sử hội thoại (newest-first → reverse để oldest-first cho API)
        List<AiMessage> history = aiMessageRepository
                .findTop20ByUserIdOrderByCreatedDateDesc(user.getId());
        Collections.reverse(history);

        // Gọi Claude API
        String assistantReply = callClaudeApi(history);

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

    // ─── Gọi Claude API ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callClaudeApi(List<AiMessage> history) {
        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", messages);

        try {
            Map<String, Object> response = aiWebClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content != null && !content.isEmpty()) {
                return (String) content.get(0).get("text");
            }

            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";

        } catch (Exception e) {
            log.error("Error calling Claude API: {}", e.getMessage());
            return "Xin lỗi, đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        }
    }

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
