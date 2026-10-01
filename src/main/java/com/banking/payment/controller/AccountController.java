package com.banking.payment.controller;

import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.exception.ErrorResponse;
import com.banking.payment.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank account lifecycle operations: onboarding, balance queries, deposits, and cash withdrawals.")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @Operation(
            summary = "Get all accounts for the authenticated user",
            description = "Retrieves all bank accounts owned by the currently authenticated customer based on their JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of user accounts retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        String username = getAuthenticatedUsername();
        return ResponseEntity.ok(accountService.getAccountsByUsername(username));
    }

    @PostMapping("/me")
    @Operation(
            summary = "Create a new bank account for the authenticated user",
            description = "Opens a new bank account (CHECKING or SAVINGS) for the currently authenticated customer."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload or negative initial balance",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AccountResponse> createMyAccount(@Valid @RequestBody CreateAccountRequest request) {
        String username = getAuthenticatedUsername();
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccountForUsername(username, request));
    }

    @PostMapping("/user/{userId}")
    @Operation(
            summary = "Create a new bank account",
            description = "Opens a new bank account (CHECKING or SAVINGS) for a customer user with an initial deposit amount."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload or negative initial balance",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found for the provided userId",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AccountResponse> createAccount(
            @Parameter(description = "ID of the user who will own the new account", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(userId, request));
    }

    @GetMapping("/{accountNumber}")
    @Operation(
            summary = "Get account details by account number",
            description = "Retrieves account metadata, status, owner information, and current balance using the unique account number."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found with the specified account number",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @Parameter(description = "Unique bank account number", example = "ACC20260927001", required = true)
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get all accounts owned by a user",
            description = "Retrieves a list of all bank accounts associated with the specified customer user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of user accounts retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found for the provided userId",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<AccountResponse>> getAccountsByUserId(
            @Parameter(description = "ID of the user whose accounts to retrieve", example = "1", required = true)
            @PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(
            summary = "Deposit funds into an account",
            description = "Credits the designated amount into the account with pessimistic write locking, guaranteeing thread-safe balance updates."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid deposit amount (must be positive)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found with the specified account number",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - Rate limit exceeded on deposits",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AccountResponse> deposit(
            @Parameter(description = "Account number to receive the deposit", example = "ACC20260927001", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositWithdrawRequest request) {
        return ResponseEntity.ok(accountService.deposit(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(
            summary = "Withdraw funds from an account",
            description = "Debits the requested amount from the bank account with pessimistic locking and validation to ensure sufficient funds exist."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Withdrawal completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Insufficient funds or invalid withdrawal amount",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT Bearer token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found with the specified account number",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - Rate limit exceeded on withdrawals",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AccountResponse> withdraw(
            @Parameter(description = "Account number to withdraw funds from", example = "ACC20260927001", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositWithdrawRequest request) {
        return ResponseEntity.ok(accountService.withdraw(accountNumber, request));
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        return authentication.getName();
    }
}
