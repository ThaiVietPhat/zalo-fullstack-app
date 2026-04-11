package com.example.backend.ai.service;

import com.example.backend.ai.entity.AiMessage;
import com.example.backend.user.entity.User;
import com.example.backend.shared.exception.ResourceNotFoundException;
import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.ai.dto.AiMessageDto;
import com.example.backend.ai.repository.AiMessageRepository;
import com.example.backend.user.repository.UserRepository;
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

    @Value("${app.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${app.groq.max-tokens:1024}")
    private int maxTokens;

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

        // Gọi Groq API
        String assistantReply = callGroqApi(history);

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

    // ─── Gọi Groq API (OpenAI-compatible) ───────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGroqApi(List<AiMessage> history) {
        // Groq dùng OpenAI format: role "user" / "assistant"
        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", maxTokens);

        try {
            Map<String, Object> response = aiWebClient.post()
                    .uri("/openai/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }

            log.warn("Groq API returned unexpected response: {}", response);
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Groq API rate limit: {}", e.getResponseBodyAsString());
                return "Trợ lý AI đang bận, vui lòng thử lại sau vài giây.";
            }
            log.error("Groq API HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return "Xin lỗi, đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        } catch (Exception e) {
            log.error("Groq API exception: {} — {}", e.getClass().getSimpleName(), e.getMessage());
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
