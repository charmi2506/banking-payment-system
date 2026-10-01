package com.banking.payment.controller;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransactionResponse;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import com.banking.payment.security.CustomUserDetailsService;
import com.banking.payment.security.JwtAuthenticationFilter;
import com.banking.payment.security.JwtTokenProvider;
import com.banking.payment.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

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
        when(rateLimitProperties.isEnabled()).thenReturn(false);
    }

    @Test
    @DisplayName("Should process fund transfer API request successfully")
    void testTransferFunds_Success() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("250.00"))
                .idempotencyKey("KEY-MOCK-1")
                .description("Mock transfer test")
                .build();

        TransferResponse response = TransferResponse.builder()
                .referenceNumber("TXN-REF-12345")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("250.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("KEY-MOCK-1")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.transferFunds(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("X-Idempotency-Key", "KEY-MOCK-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value("TXN-REF-12345"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should retrieve paginated transaction history for authenticated user")
    void testGetTransactionHistory_Pagination() throws Exception {
        TransactionResponse txn = TransactionResponse.builder()
                .referenceNumber("TXN-001")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        Page<TransactionResponse> page = new PageImpl<>(List.of(txn), PageRequest.of(0, 10), 1);
        when(transactionService.getTransactionHistory(eq("testuser"), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/history")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].referenceNumber").value("TXN-001"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should retrieve filtered transaction history by type, status, and dates")
    void testGetTransactionHistory_WithFilters() throws Exception {
        TransactionResponse txn = TransactionResponse.builder()
                .referenceNumber("TXN-FILTERED")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("75.50"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        Page<TransactionResponse> page = new PageImpl<>(List.of(txn), PageRequest.of(0, 10), 1);
        when(transactionService.getTransactionHistory(eq("testuser"), eq("ACC10001"), eq(TransactionType.TRANSFER),
                eq(TransactionStatus.COMPLETED), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/history")
                        .param("accountNumber", "ACC10001")
                        .param("type", "TRANSFER")
                        .param("status", "COMPLETED")
                        .param("startDate", "2026-09-01T00:00:00")
                        .param("endDate", "2026-09-27T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceNumber").value("TXN-FILTERED"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should return empty result page when no transactions exist")
    void testGetTransactionHistory_EmptyResults() throws Exception {
        Page<TransactionResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(transactionService.getTransactionHistory(eq("testuser"), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/transactions/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when unauthenticated request accesses transaction history")
    void testGetTransactionHistory_Unauthenticated_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should return 403 Forbidden when user attempts to access account they do not own")
    void testGetTransactionHistory_AccountNotOwned_Forbidden() throws Exception {
        when(transactionService.getTransactionHistory(eq("testuser"), eq("ACC99999"), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("Access denied: You do not own account ACC99999"));

        mockMvc.perform(get("/api/v1/transactions/history")
                        .param("accountNumber", "ACC99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should retrieve account statement using /statements endpoint alias")
    void testGetStatements_Success() throws Exception {
        TransactionResponse txn = TransactionResponse.builder()
                .referenceNumber("TXN-STMT-001")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        Page<TransactionResponse> page = new PageImpl<>(List.of(txn), PageRequest.of(0, 10), 1);
        when(transactionService.getTransactionHistory(eq("testuser"), eq("ACC10001"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/statements")
                        .param("accountNumber", "ACC10001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceNumber").value("TXN-STMT-001"));
    }
}
