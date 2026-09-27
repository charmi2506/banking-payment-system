package com.banking.payment.service;

import com.banking.payment.dto.request.AuthRequest;
import com.banking.payment.dto.request.RegisterRequest;
import com.banking.payment.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    AuthResponse register(RegisterRequest request);
}
