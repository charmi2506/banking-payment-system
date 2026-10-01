package com.banking.payment.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardized error response payload returned on API failures")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Short HTTP error category name", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed explanation of the error", example = "Insufficient funds in source account ACC10001")
    private String message;

    @Schema(description = "List of specific field validation errors or failure details", example = "[\"Amount must be greater than zero\"]")
    private List<String> details;

    @Schema(description = "Timestamp when the error occurred", example = "2026-09-27T12:45:00")
    private LocalDateTime timestamp;
}
