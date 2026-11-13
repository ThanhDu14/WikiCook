package com.wikicook.auth.controller;

import com.wikicook.auth.dto.AuthResponse;
import com.wikicook.auth.dto.CurrentAccountDto;
import com.wikicook.auth.dto.LoginRequest;
import com.wikicook.auth.dto.RegisterRequest;
import com.wikicook.auth.dto.RegisterResponse;
import com.wikicook.auth.security.RefreshCookie;
import com.wikicook.auth.service.AccountSessionService;
import com.wikicook.auth.service.AccountSessionService.Refreshed;
import com.wikicook.auth.service.RegistrationService;
import com.wikicook.auth.service.SessionService;
import com.wikicook.auth.service.SessionService.Status;
import com.wikicook.auth.service.SignInService;
import com.wikicook.auth.service.SignInService.SignInCommand;
import com.wikicook.auth.service.SignInService.SignInResult;
import com.wikicook.common.security.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign in, refresh, sign out and the current account (contracts/openapi.yaml, tag {@code auth}). The refresh token is
 * only ever in the {@code wc_rt} cookie; {@code /refresh} and {@code /logout} read it and therefore need the
 * {@code X-WikiCook-Client} header, checked by {@code ClientHeaderFilter} before a request gets here.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String USER_AGENT = "User-Agent";

    private final SignInService signIn;
    private final RegistrationService registration;
    private final AccountSessionService accounts;
    private final SessionService sessions;
    private final RefreshCookie cookie;
    private final CurrentAccount current;

    public AuthController(SignInService signIn, RegistrationService registration, AccountSessionService accounts, SessionService sessions,
            RefreshCookie cookie, CurrentAccount current) {
        this.signIn = signIn;
        this.registration = registration;
        this.accounts = accounts;
        this.sessions = sessions;
        this.cookie = cookie;
        this.current = current;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registration.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        SignInResult result = signIn.signIn(new SignInCommand(request.email(), request.password(),
                request.rememberMe(), clientIp(http), http.getHeader(USER_AGENT)));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.issue(result.session().refreshToken(), request.rememberMe(),
                        result.session().session().getExpiresAt()))
                .body(new AuthResponse(result.accessToken().value(), result.accessToken().expiresInSeconds(),
                        CurrentAccountDto.of(result.account())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        Refreshed refreshed = accounts.refresh(refreshToken);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (refreshed.result().status() == Status.ROTATED) {
            response.header(HttpHeaders.SET_COOKIE, cookie.issue(refreshed.result().newRefreshToken(),
                    refreshed.result().session().isRememberMe(), refreshed.result().session().getExpiresAt()));
        }
        return response.body(new AuthResponse(refreshed.accessToken().value(),
                refreshed.accessToken().expiresInSeconds(), CurrentAccountDto.of(refreshed.account())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessions.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.clear()).build();
    }

    @GetMapping("/me")
    public CurrentAccountDto me() {
        return accounts.currentAccount(current.id());
    }

    /** The caller's address; {@code server.forward-headers-strategy=native} makes it the real client behind nginx. */
    private static InetAddress clientIp(HttpServletRequest request) {
        try {
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
