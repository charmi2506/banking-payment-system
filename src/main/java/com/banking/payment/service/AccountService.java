package com.banking.payment.service;

import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(Long userId, CreateAccountRequest request);
    AccountResponse getAccountByNumber(String accountNumber);
    List<AccountResponse> getAccountsByUserId(Long userId);
    AccountResponse deposit(String accountNumber, DepositWithdrawRequest request);
    AccountResponse withdraw(String accountNumber, DepositWithdrawRequest request);
}
