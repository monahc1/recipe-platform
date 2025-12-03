package com.flavorshare.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flavorshare.dto.LoginRequest;
import com.flavorshare.dto.SignupRequest;
import com.flavorshare.model.User;
import com.flavorshare.repo.UserRepository;
import com.flavorshare.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests authentication endpoints: signup, login, getCurrentUser
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Authentication Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private final String TEST_PASSWORD = "SecurePass123!";

    @BeforeEach
    void setUp() {
        // Clean database
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);
    }

    // ==================== SIGNUP TESTS ====================

    @Test
    @DisplayName("Should signup new user successfully")
    void shouldSignupNewUser() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("NewPass123!");
        signupRequest.setFullName("New User");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.fullName").value("New User"))
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    @DisplayName("Should reject signup with duplicate username")
    void shouldRejectDuplicateUsername() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(testUser.getUsername()); // Duplicate
        signupRequest.setEmail("different@example.com");
        signupRequest.setPassword("Pass123!");
        signupRequest.setFullName("Another User");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already exists")));
    }

    @Test
    @DisplayName("Should reject signup with duplicate email")
    void shouldRejectDuplicateEmail() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("differentuser");
        signupRequest.setEmail(testUser.getEmail()); // Duplicate
        signupRequest.setPassword("Pass123!");
        signupRequest.setFullName("Another User");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Email already exists")));
    }

    @Test
    @DisplayName("Should reject signup with missing username")
    void shouldRejectSignupWithMissingUsername() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("Pass123!");
        signupRequest.setFullName("Test");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject signup with invalid email")
    void shouldRejectSignupWithInvalidEmail() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("not-an-email"); // Invalid
        signupRequest.setPassword("Pass123!");
        signupRequest.setFullName("Test");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject signup with short password")
    void shouldRejectSignupWithShortPassword() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("short"); // Too short
        signupRequest.setFullName("Test");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login with correct credentials")
    void shouldLoginWithCorrectCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getUsername());
        loginRequest.setPassword(TEST_PASSWORD);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getUsername());
        loginRequest.setPassword("WrongPassword123!");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid credentials")));
    }

    @Test
    @DisplayName("Should reject login with non-existent username")
    void shouldRejectLoginWithNonExistentUsername() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("Pass123!");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid credentials")));
    }

    @Test
    @DisplayName("Should reject login with missing credentials")
    void shouldRejectLoginWithMissingCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        // No username or password

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET CURRENT USER TESTS ====================

    @Test
    @DisplayName("Should get current user with valid token")
    void shouldGetCurrentUserWithValidToken() throws Exception {
        // Given
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    @DisplayName("Should reject request without token")
    void shouldRejectRequestWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with invalid token")
    void shouldRejectRequestWithInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject request with malformed Authorization header")
    void shouldRejectRequestWithMalformedHeader() throws Exception {
        // Given
        String token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());

        // When & Then - Missing "Bearer " prefix
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PASSWORD HASHING TESTS ====================

    @Test
    @DisplayName("Should hash password during signup")
    void shouldHashPasswordDuringSignup() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("hashtest");
        signupRequest.setEmail("hash@example.com");
        signupRequest.setPassword("PlainPassword123!");
        signupRequest.setFullName("Hash Test");

        // When
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Then - Verify password is hashed in database
        User savedUser = userRepository.findByUsername("hashtest").orElseThrow();
        assert !savedUser.getPassword().equals("PlainPassword123!");
        assert passwordEncoder.matches("PlainPassword123!", savedUser.getPassword());
    }
}
