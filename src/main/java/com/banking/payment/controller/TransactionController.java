package com.banking.payment.controller;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransactionResponse;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import com.banking.payment.exception.ErrorResponse;
import com.banking.payment.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Money transfer operations featuring pessimistic locking, ACID guarantees, and idempotency tracking.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(
            summary = "Transfer funds between accounts",
            description = "Executes an atomic transfer of funds between two bank accounts. " +
                    "Applies deterministic pessimistic locking (ordered by account number) to eliminate deadlocks and race conditions. " +
                    "Guarantees idempotency via `X-Idempotency-Key` HTTP header or the request body idempotency key to prevent double debits."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Fund transfer completed successfully (or previously completed idempotent result returned)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransferResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid transfer request (insufficient funds, same source and destination, or validation error)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Source or destination account not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency conflict: transaction with this key is already in progress or completed with different payload",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - Rate limit exceeded on fund transfers",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TransferResponse> transferFunds(
            @Parameter(
                    description = "Unique idempotency key to prevent duplicate transfers on retries. Overrides body idempotencyKey if provided.",
                    example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
            )
            @RequestHeader(value = "X-Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        if (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank()) {
            request.setIdempotencyKey(headerIdempotencyKey);
        }

        return ResponseEntity.ok(transactionService.transferFunds(request));
    }

    @GetMapping("/reference/{referenceNumber}")
    @Operation(
            summary = "Get transaction details by reference number",
            description = "Retrieves complete transaction details including transfer amount, source/destination accounts, execution status, and timestamp using the unique reference number."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction details retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransferResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found for the provided reference number",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TransferResponse> getTransactionByReference(
            @Parameter(description = "Global unique transaction reference number", example = "TXN-9A8B7C6D-1234", required = true)
            @PathVariable String referenceNumber) {
        return ResponseEntity.ok(transactionService.getTransactionByReference(referenceNumber));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get transaction history for authenticated user",
            description = "Retrieves paginated transaction records belonging only to accounts owned by the authenticated user. " +
                    "Supports filtering by specific account number, transaction type, transaction status, and date range. " +
                    "Transactions are ordered by newest first."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction history retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters (e.g. startDate after endDate)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requested account does not belong to authenticated user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @Parameter(description = "Optional account number to filter statements for a specific account. Must belong to the authenticated user.", example = "ACC10001")
            @RequestParam(required = false) String accountNumber,
            @Parameter(description = "Filter by transaction classification (DEPOSIT, WITHDRAWAL, TRANSFER)", example = "TRANSFER")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by transaction status (PENDING, COMPLETED, FAILED)", example = "COMPLETED")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter transactions starting from this timestamp (ISO 8601)", example = "2026-09-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter transactions up to this timestamp (ISO 8601)", example = "2026-09-27T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @ParameterObject Pageable pageable) {

        String username = getAuthenticatedUsername();
        return ResponseEntity.ok(transactionService.getTransactionHistory(
                username, accountNumber, type, status, startDate, endDate, pageable));
    }

    @GetMapping("/statements")
    @Operation(
            summary = "Get account statements for authenticated user",
            description = "Provides paginated account statements for accounts owned by the authenticated user, supporting optional account, type, status, and date range filters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account statements retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requested account does not belong to authenticated user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Page<TransactionResponse>> getStatements(
            @Parameter(description = "Optional account number to filter statements for a specific account. Must belong to the authenticated user.", example = "ACC10001")
            @RequestParam(required = false) String accountNumber,
            @Parameter(description = "Filter by transaction classification (DEPOSIT, WITHDRAWAL, TRANSFER)", example = "TRANSFER")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Filter by transaction status (PENDING, COMPLETED, FAILED)", example = "COMPLETED")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter transactions starting from this timestamp (ISO 8601)", example = "2026-09-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter transactions up to this timestamp (ISO 8601)", example = "2026-09-27T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @ParameterObject Pageable pageable) {

        return getTransactionHistory(accountNumber, type, status, startDate, endDate, pageable);
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        return authentication.getName();
    }
}
