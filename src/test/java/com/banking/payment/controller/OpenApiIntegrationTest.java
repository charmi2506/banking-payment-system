package com.banking.payment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should successfully serve OpenAPI v3 documentation in JSON format")
    void testOpenApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Banking & Payment Transaction System API"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/user/{userId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountNumber}/deposit']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountNumber}/withdraw']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions/transfer']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions/history']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transactions/statements']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    @DisplayName("Should redirect /swagger-ui.html to Swagger UI web page")
    void testSwaggerUiRedirect() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
