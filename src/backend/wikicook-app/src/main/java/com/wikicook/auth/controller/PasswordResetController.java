package com.wikicook.auth.controller;

import com.wikicook.auth.dto.ConfirmTokenRequest;
import com.wikicook.auth.dto.ForgotPasswordRequest;
import com.wikicook.auth.dto.ResetPasswordRequest;
import com.wikicook.auth.dto.ResetTokenInfo;
import com.wikicook.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Password reset endpoints (contracts/openapi.yaml, tag {@code password}); all three are public. */
@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwords;

    public PasswordResetController(PasswordResetService passwords) {
        this.passwords = passwords;
    }

    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.requestReset(request.email());
    }

    @PostMapping("/reset/validate")
    public ResetTokenInfo validate(@Valid @RequestBody ConfirmTokenRequest request) {
        return passwords.validate(request.token());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwords.reset(request.token(), request.newPassword());
    }
}
