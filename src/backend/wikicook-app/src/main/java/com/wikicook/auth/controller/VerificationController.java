package com.wikicook.auth.controller;

import com.wikicook.auth.dto.ConfirmTokenRequest;
import com.wikicook.auth.dto.ResendVerificationRequest;
import com.wikicook.auth.dto.VerificationResult;
import com.wikicook.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Email verification endpoints (contracts/openapi.yaml, tag {@code verification}); both are public. */
@RestController
@RequestMapping("/api/v1/auth/verification")
public class VerificationController {

    private final EmailVerificationService verification;

    public VerificationController(EmailVerificationService verification) {
        this.verification = verification;
    }

    @PostMapping("/confirm")
    public VerificationResult confirm(@Valid @RequestBody ConfirmTokenRequest request) {
        return verification.confirm(request.token());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@Valid @RequestBody ResendVerificationRequest request) {
        verification.resend(request);
    }
}
