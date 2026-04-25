package com.example.backend.ai.service;

import com.example.backend.ai.dto.SmartReplyResponse;
import com.example.backend.ai.dto.SummarizeResponse;
import com.example.backend.chat.repository.ChatRepository;
import com.example.backend.messaging.dto.MessageDto;
import com.example.backend.messaging.entity.Message;
import com.example.backend.messaging.enums.MessageState;
import com.example.backend.messaging.enums.MessageType;
import com.example.backend.messaging.repository.MessageRepository;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatAiService {

    // UUID cố định cho AI Bot user (được seed qua V21 migration)
    public static final UUID AI_BOT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String AI_BOT_NAME = "Trợ lý AI";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final int CONTEXT_MSG_LIMIT = 30;
    private static final int SUMMARIZE_MAX_MSGS = 50;

    private final ChatClient chatClient;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatAiService(ChatClient.Builder chatClientBuilder,
                         MessageRepository messageRepository,
                         ChatRepository chatRepository,
                         UserRepository userRepository,
                         SimpMessagingTemplate messagingTemplate,
                         ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Bạn là trợ lý AI thông minh tích hợp trong ứng dụng chat Zalo Clone.
                        Hãy trả lời bằng tiếng Việt khi người dùng viết tiếng Việt,
                        và bằng tiếng Anh khi họ viết tiếng Anh.
                        Câu trả lời nên ngắn gọn, rõ ràng và hữu ích.
                        """)
                .build();
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    // ─── Feature 1: Smart Reply ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SmartReplyResponse getSmartReplies(UUID chatId) {
        List<Message> messages = messageRepository
                .findRecentTextMessagesForAi(chatId, PageRequest.of(0, CONTEXT_MSG_LIMIT));
        Collections.reverse(messages);

        if (messages.isEmpty()) {
            return SmartReplyResponse.builder()
                    .suggestions(List.of("Được rồi!", "OK", "Cho tôi biết thêm nhé"))
                    .build();
        }

        String context = buildMessageContext(messages);
        String lastMsg = messages.get(messages.size() - 1).getContent();

        String prompt = """
                Đây là lịch sử cuộc trò chuyện:
                ---
                %s
                ---
                Tin nhắn cuối cùng: "%s"

                Hãy gợi ý ĐÚNG 3 câu trả lời ngắn, tự nhiên, phù hợp ngữ cảnh cho tin nhắn cuối.
                Trả lời ĐÚNG định dạng JSON array (không giải thích thêm gì):
                ["câu 1", "câu 2", "câu 3"]
                """.formatted(context, lastMsg);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            List<String> suggestions = parseJsonArraySafe(raw);
            return SmartReplyResponse.builder().suggestions(suggestions).build();
        } catch (Exception e) {
            log.warn("Chat smart reply AI call failed: {}", e.getMessage());
            return SmartReplyResponse.builder()
                    .suggestions(List.of("Được rồi!", "OK bạn ơi", "Cho tôi xem thêm"))
                    .build();
        }
    }

    // ─── Feature 2: Summarize ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SummarizeResponse summarize(UUID chatId, LocalDateTime since) {
        LocalDateTime to = LocalDateTime.now();

        List<Message> messages = messageRepository
                .findMessagesForAiByDateRange(chatId, since, to);

        if (messages.isEmpty()) {
            return SummarizeResponse.builder()
                    .summary("Không có tin nhắn nào trong khoảng thời gian này.")
                    .messageCount(0)
                    .topSpeakers(List.of())
                    .from(since)
                    .to(to)
                    .build();
        }

        List<Message> sample = messages.size() > SUMMARIZE_MAX_MSGS
                ? messages.subList(messages.size() - SUMMARIZE_MAX_MSGS, messages.size())
                : messages;

        String context = sample.stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> "[%s] %s: %s".formatted(
                        m.getCreatedDate().format(TIME_FMT),
                        m.getSender().getFirstName() + " " + m.getSender().getLastName(),
                        m.getContent()))
                .collect(Collectors.joining("\n"));

        String prompt = """
                Đây là lịch sử cuộc trò chuyện từ %s đến %s (%d tin nhắn):
                ---
                %s
                ---

                Hãy tóm tắt cuộc trò chuyện trên ngắn gọn (2-4 câu), nêu:
                - Các chủ đề chính được thảo luận
                - Kết luận hoặc quyết định quan trọng (nếu có)
                Trả lời bằng tiếng Việt.
                """.formatted(since.format(TIME_FMT), to.format(TIME_FMT), messages.size(), context);

        String summary;
        try {
            summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Chat summarize AI call failed: {}", e.getMessage());
            summary = "Không thể tạo tóm tắt lúc này. Vui lòng thử lại sau.";
        }

        return SummarizeResponse.builder()
                .summary(summary)
                .messageCount(messages.size())
                .topSpeakers(List.of()) // Chat 1-1 không cần top speakers
                .from(since)
                .to(to)
                .build();
    }

    // ─── Feature 3: @AI Bot ───────────────────────────────────────────────────

    @Async
    @Transactional
    public void handleBotMentionAsync(UUID chatId, String messageContent, String senderName) {
        log.info("AI Bot processing @ai mention in chat {} from {}", chatId, senderName);

        List<Message> contextMsgs = messageRepository
                .findRecentTextMessagesForAi(chatId, PageRequest.of(0, CONTEXT_MSG_LIMIT));
        Collections.reverse(contextMsgs);

        String question = messageContent
                .replaceAll("(?i)@ai\\b", "")
                .trim();
        if (question.isEmpty()) question = "Xin chào!";

        String systemContext = contextMsgs.isEmpty() ? "" :
                "Context cuộc trò chuyện gần đây:\n" + buildMessageContext(contextMsgs) + "\n\n";

        String userPrompt = systemContext +
                "Người dùng " + senderName + " hỏi: " + question + "\n\n" +
                "Hãy trả lời ngắn gọn, thân thiện, hữu ích. " +
                "Dùng tiếng Việt nếu câu hỏi bằng tiếng Việt.";

        String reply;
        try {
            reply = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Chat bot mention AI call failed: {}", e.getMessage());
            reply = "Xin lỗi, tôi đang bận. Vui lòng thử lại sau nhé!";
        }

        User botUser = userRepository.findById(AI_BOT_USER_ID).orElse(null);
        if (botUser == null) {
            log.error("AI Bot user not found in DB (id={}). Hãy chạy migration V21.", AI_BOT_USER_ID);
            return;
        }

        com.example.backend.chat.entity.Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            log.warn("Chat {} not found, aborting AI bot reply", chatId);
            return;
        }

        // Lưu tin nhắn bot vào DB
        Message botMsg = new Message();
        botMsg.setChat(chat);
        botMsg.setSender(botUser);
        botMsg.setContent(reply);
        botMsg.setType(MessageType.TEXT);
        botMsg.setState(MessageState.SENT);
        Message saved = messageRepository.save(botMsg);

        // Build DTO thủ công (không dùng mapper vì bot không phải user1/user2 của chat)
        MessageDto dto = MessageDto.builder()
                .id(saved.getId())
                .chatId(chatId)
                .content(saved.getContent())
                .type(MessageType.TEXT)
                .state(MessageState.SENT)
                .senderId(AI_BOT_USER_ID)
                .senderName(AI_BOT_NAME)
                .deleted(false)
                .reactions(List.of())
                .build();

        // Broadcast đến cả 2 người trong chat
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, dto);
        log.info("AI Bot replied to chat {}", chatId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildMessageContext(List<Message> messages) {
        return messages.stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> {
                    String name = m.getSender().getId().equals(AI_BOT_USER_ID)
                            ? AI_BOT_NAME
                            : m.getSender().getFirstName() + " " + m.getSender().getLastName();
                    return name + ": " + m.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    private List<String> parseJsonArraySafe(String raw) {
        try {
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String arr = raw.substring(start, end + 1);
                return objectMapper.readValue(arr, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse smart reply JSON, raw={}", raw);
        }
        return List.of("OK", "Được rồi!", "Để tôi xem lại");
    }
}
