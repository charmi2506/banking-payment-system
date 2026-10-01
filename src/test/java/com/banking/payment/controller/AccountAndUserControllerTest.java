package com.banking.payment.controller;

import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.dto.response.UserResponse;
import com.banking.payment.entity.User;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
import com.banking.payment.entity.enums.Role;
import com.banking.payment.mapper.UserMapper;
import com.banking.payment.repository.UserRepository;
import com.banking.payment.security.CustomUserDetailsService;
import com.banking.payment.security.JwtAuthenticationFilter;
import com.banking.payment.security.JwtTokenProvider;
import com.banking.payment.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AccountController.class, UserController.class})
@AutoConfigureMockMvc(addFilters = false)
class AccountAndUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.banking.payment.service.RateLimiterService rateLimiterService;

    @MockBean
    private com.banking.payment.config.RateLimitProperties rateLimitProperties;

    @org.junit.jupiter.api.BeforeEach
    void setUpRateLimit() {
        org.mockito.Mockito.when(rateLimitProperties.isEnabled()).thenReturn(false);
    }

    @Test
    @WithMockUser(username = "johndoe", roles = {"USER"})
    @DisplayName("GET /api/v1/users/me returns authenticated user profile")
    void testGetCurrentUser() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = {"USER"})
    @DisplayName("GET /api/v1/accounts/me returns all accounts for authenticated user")
    void testGetMyAccounts() throws Exception {
        AccountResponse acc1 = AccountResponse.builder()
                .accountNumber("ACC10001")
                .balance(new BigDecimal("1500.00"))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .ownerUsername("johndoe")
                .build();

        when(accountService.getAccountsByUsername("johndoe")).thenReturn(List.of(acc1));

        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("ACC10001"))
                .andExpect(jsonPath("$[0].balance").value(1500.00))
                .andExpect(jsonPath("$[0].type").value("CHECKING"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = {"USER"})
    @DisplayName("POST /api/v1/accounts/me creates account for authenticated user")
    void testCreateMyAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setType(AccountType.SAVINGS);
        request.setInitialBalance(new BigDecimal("500.00"));

        AccountResponse created = AccountResponse.builder()
                .accountNumber("ACC10002")
                .balance(new BigDecimal("500.00"))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .ownerUsername("johndoe")
                .build();

        when(accountService.createAccountForUsername(eq("johndoe"), any(CreateAccountRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC10002"))
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }
}
