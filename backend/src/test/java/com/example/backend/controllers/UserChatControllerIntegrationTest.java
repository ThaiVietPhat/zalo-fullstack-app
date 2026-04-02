package com.example.backend.controllers;

import com.example.backend.models.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserController + ChatController Integration Tests")
class UserChatControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static String tokenUser1;
    private static String tokenUser2;
    private static String user2Id;
    private static String chatId;

    @BeforeAll
    static void setup(@Autowired MockMvc mockMvc,
                      @Autowired ObjectMapper objectMapper) throws Exception {
        // Tạo user1
        AuthRequest.Register req1 = AuthRequest.Register.builder()
                .email("chat_user1@gmail.com").password("123456")
                .firstName("Chat").lastName("User1").build();
        MvcResult r1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andReturn();
        tokenUser1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Tạo user2
        AuthRequest.Register req2 = AuthRequest.Register.builder()
                .email("chat_user2@gmail.com").password("123456")
                .firstName("Chat").lastName("User2").build();
        MvcResult r2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn();
        tokenUser2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .get("accessToken").asText();
        user2Id = objectMapper.readTree(r2.getResponse().getContentAsString())
                .get("userId").asText();
    }

    // ─── User API ─────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("GET /user - lấy danh sách user thành công")
    void getUsers_success() throws Exception {
        mockMvc.perform(get("/api/v1/user")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(2)
    @DisplayName("GET /user - không có token → 401")
    void getUsers_noToken() throws Exception {
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(3)
    @DisplayName("GET /user/me - lấy profile bản thân")
    void getMyProfile_success() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("chat_user1@gmail.com"));
    }

    @Test @Order(4)
    @DisplayName("GET /user/search - tìm kiếm user")
    void searchUsers_success() throws Exception {
        mockMvc.perform(get("/api/v1/user/search?keyword=Chat")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(5)
    @DisplayName("GET /user/search - không có kết quả")
    void searchUsers_noResult() throws Exception {
        mockMvc.perform(get("/api/v1/user/search?keyword=xyzxyzxyz")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── Chat API ─────────────────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("POST /chat/start - tạo chat thành công")
    void startChat_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/chat/start/" + user2Id)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        chatId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(7)
    @DisplayName("POST /chat/start - gọi lại → trả về chat cũ")
    void startChat_returnsExisting() throws Exception {
        mockMvc.perform(post("/api/v1/chat/start/" + user2Id)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chatId));
    }

    @Test @Order(8)
    @DisplayName("GET /chat - lấy danh sách chat")
    void getChats_success() throws Exception {
        mockMvc.perform(get("/api/v1/chat")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test @Order(9)
    @DisplayName("GET /chat/{chatId} - lấy chi tiết chat")
    void getChatById_success() throws Exception {
        mockMvc.perform(get("/api/v1/chat/" + chatId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chatId));
    }

    @Test @Order(10)
    @DisplayName("GET /message/chat/{chatId} - lấy tin nhắn trong chat (paginated)")
    void getChatMessages_success() throws Exception {
        mockMvc.perform(get("/api/v1/message/chat/" + chatId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(11)
    @DisplayName("PATCH /message/seen/{chatId} - đánh dấu đã đọc")
    void markAsRead_success() throws Exception {
        mockMvc.perform(patch("/api/v1/message/seen/" + chatId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk());
    }

    @Test @Order(12)
    @DisplayName("GET /chat/{chatId} - user khác không có quyền → 403")
    void getChatById_wrongUser_fails() throws Exception {
        // Tạo user3 không liên quan
        AuthRequest.Register req3 = AuthRequest.Register.builder()
                .email("chat_user3@gmail.com").password("123456")
                .firstName("Chat").lastName("User3").build();
        MvcResult r3 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andReturn();
        String token3 = objectMapper.readTree(r3.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/chat/" + chatId)
                        .header("Authorization", "Bearer " + token3))
                .andExpect(status().isForbidden());
    }
}