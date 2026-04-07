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

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${app.gemini.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.gemini.context-turns:20}")
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

        // Gọi Gemini API
        String assistantReply = callGeminiApi(history);

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

    // ─── Gọi Gemini API ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGeminiApi(List<AiMessage> history) {
        // Gemini dùng role "user" và "model" (không phải "assistant")
        List<Map<String, Object>> contents = history.stream()
                .map(m -> {
                    String geminiRole = "assistant".equals(m.getRole()) ? "model" : "user";
                    Map<String, String> part = Map.of("text", m.getContent());
                    return Map.<String, Object>of(
                            "role", geminiRole,
                            "parts", List.of(part)
                    );
                })
                .collect(Collectors.toList());

        Map<String, Object> generationConfig = Map.of("maxOutputTokens", maxTokens);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        requestBody.put("generationConfig", generationConfig);

        try {
            Map<String, Object> response = aiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + model + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            log.warn("Gemini API returned unexpected response: {}", response);
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Gemini API rate limit: {}", e.getResponseBodyAsString());
                return "Trợ lý AI đang bận, vui lòng thử lại sau vài giây.";
            }
            log.error("Gemini API HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return "Xin lỗi, đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        } catch (Exception e) {
            log.error("Gemini API exception: {} — {}", e.getClass().getSimpleName(), e.getMessage());
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
