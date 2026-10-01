package com.banking.payment.controller;

import com.banking.payment.dto.request.AuthRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RateLimitingMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("POST /api/v1/auth/login should return HTTP 429 when rate limit is exceeded")
    void testLogin_RateLimitExceeded_Returns429() throws Exception {
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        AuthRequest request = AuthRequest.builder()
                .usernameOrEmail("user1")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Rate limit exceeded")));
    }

    @Test
    @WithMockUser(username = "transfer_user")
    @DisplayName("POST /api/v1/transactions/transfer should return HTTP 429 when rate limit is exceeded")
    void testTransfer_RateLimitExceeded_Returns429() throws Exception {
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("KEY-RATE-1")
                .build();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @WithMockUser(username = "deposit_user")
    @DisplayName("POST /api/v1/accounts/{accountNumber}/deposit should return HTTP 429 when rate limit is exceeded")
    void testDeposit_RateLimitExceeded_Returns429() throws Exception {
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                .amount(new BigDecimal("50.00"))
                .description("Deposit test")
                .build();

        mockMvc.perform(post("/api/v1/accounts/ACC10001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @WithMockUser(username = "withdraw_user")
    @DisplayName("POST /api/v1/accounts/{accountNumber}/withdraw should return HTTP 429 when rate limit is exceeded")
    void testWithdraw_RateLimitExceeded_Returns429() throws Exception {
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                .amount(new BigDecimal("50.00"))
                .description("Withdraw test")
                .build();

        mockMvc.perform(post("/api/v1/accounts/ACC10001/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }
}
