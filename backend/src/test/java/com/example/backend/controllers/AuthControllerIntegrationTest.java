package com.example.backend.controllers;

import com.example.backend.models.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static String accessToken;
    private static String refreshToken;
    private static final String TEST_EMAIL    = "itest_user@gmail.com";
    private static final String TEST_PASSWORD = "123456";

    // ─── Register ────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /register - thành công")
    void register_success() throws Exception {
        AuthRequest.Register req = AuthRequest.Register.builder()
                .email(TEST_EMAIL).password(TEST_PASSWORD)
                .firstName("ITest").lastName("User").build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken  = objectMapper.readTree(body).get("accessToken").asText();
        refreshToken = objectMapper.readTree(body).get("refreshToken").asText();
    }

    @Test @Order(2)
    @DisplayName("POST /register - email đã tồn tại → 409")
    void register_duplicate_email() throws Exception {
        AuthRequest.Register req = AuthRequest.Register.builder()
                .email(TEST_EMAIL).password(TEST_PASSWORD)
                .firstName("ITest").lastName("User").build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    @DisplayName("POST /register - thiếu email → 400")
    void register_missingEmail() throws Exception {
        AuthRequest.Register req = AuthRequest.Register.builder()
                .email("").password(TEST_PASSWORD)
                .firstName("ITest").lastName("User").build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("POST /login - thành công")
    void login_success() throws Exception {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email(TEST_EMAIL).password(TEST_PASSWORD).build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test @Order(5)
    @DisplayName("POST /login - sai mật khẩu → 401")
    void login_wrongPassword() throws Exception {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email(TEST_EMAIL).password("wrongpass").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(6)
    @DisplayName("POST /login - email không tồn tại → 401")
    void login_unknownEmail() throws Exception {
        AuthRequest.Login req = AuthRequest.Login.builder()
                .email("nobody@gmail.com").password("123456").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("POST /refresh - thành công")
    void refresh_success() throws Exception {
        AuthRequest.RefreshToken req = new AuthRequest.RefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test @Order(8)
    @DisplayName("POST /refresh - token rác → 401")
    void refresh_invalidToken() throws Exception {
        AuthRequest.RefreshToken req = new AuthRequest.RefreshToken("invalid.token.here");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("POST /logout - thành công")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test @Order(10)
    @DisplayName("POST /logout - không có token → 401")
    void logout_noToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}