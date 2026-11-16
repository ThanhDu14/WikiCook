package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.auth.service.AuthErrorCode;
import com.wikicook.auth.service.AuthException;
import com.wikicook.mealplan.controller.MealPlanValidationProbe;
import com.wikicook.support.ForeignModuleProbe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Research R15, NFR-007, constitution IV: every exception becomes a ProblemDetail with the status and {@code code} of
 * contracts/openapi.yaml, field errors carry {@code errors[].code}, nothing leaks a stack trace or class name, and the
 * two advices cooperate with {@code GlobalExceptionHandler} and 030's handler instead of fighting them.
 */
@WebMvcTest(controllers = AuthExceptionHandlerTest.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuthExceptionHandlerTest.AuthProbe.class, ForeignModuleProbe.class, MealPlanValidationProbe.class})
class AuthExceptionHandlerTest {

    private static final Instant WHEN = Instant.parse("2026-11-13T04:00:00Z");

    @Autowired
    MockMvc mvc;

    /** A controller inside the auth package, so {@code AuthExceptionHandler} applies to it. */
    @RestController
    @RequestMapping("/api/v1/auth-probe")
    static class AuthProbe {

        record Form(@NotBlank String displayName, @Email String email, @Size(max = 3) String nickname) {
        }

        @GetMapping("/code/{code}")
        void code(@org.springframework.web.bind.annotation.PathVariable AuthErrorCode code) {
            throw new AuthException(code, "detail for " + code);
        }

        @GetMapping("/locked")
        void locked() {
            throw AuthException.accountLocked(WHEN);
        }

        @GetMapping("/suspended")
        void suspended() {
            throw AuthException.accountSuspended("Spam", WHEN);
        }

        @GetMapping("/banned")
        void banned() {
            throw AuthException.accountBanned("Fraud", "closure-token-123");
        }

        @GetMapping("/email-rate-limited")
        void emailRateLimited() {
            throw AuthException.emailRateLimited(900);
        }

        @GetMapping("/crash")
        void crash() {
            throw new IllegalStateException("secret internal detail com.wikicook.auth.Internal");
        }

        @PostMapping("/form")
        Map<String, Object> form(@Valid @RequestBody Form form) {
            return Map.of("ok", true);
        }
    }

    static Stream<Arguments> everyCode() {
        return Stream.of(
                Arguments.of(AuthErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST),
                Arguments.of(AuthErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
                Arguments.of(AuthErrorCode.INVALID_CLIENT, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
                Arguments.of(AuthErrorCode.ACCOUNT_LOCKED, HttpStatus.LOCKED),
                Arguments.of(AuthErrorCode.ACCOUNT_SUSPENDED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.ACCOUNT_BANNED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.EMAIL_TAKEN, HttpStatus.CONFLICT),
                Arguments.of(AuthErrorCode.REGISTRATION_REFUSED, HttpStatus.UNPROCESSABLE_ENTITY),
                Arguments.of(AuthErrorCode.EMAIL_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS),
                Arguments.of(AuthErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS),
                Arguments.of(AuthErrorCode.TOKEN_EXPIRED, HttpStatus.GONE),
                Arguments.of(AuthErrorCode.TOKEN_INVALID, HttpStatus.GONE),
                Arguments.of(AuthErrorCode.EMAIL_NOT_VERIFIED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.REAUTH_REQUIRED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.SELF_ACTION_FORBIDDEN, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.LAST_ADMINISTRATOR, HttpStatus.CONFLICT),
                Arguments.of(AuthErrorCode.ACCOUNT_NOT_ACTIVE, HttpStatus.CONFLICT),
                Arguments.of(AuthErrorCode.ACCOUNT_NOT_SANCTIONED, HttpStatus.CONFLICT),
                Arguments.of(AuthErrorCode.SUSPENSION_LIMIT_EXCEEDED, HttpStatus.FORBIDDEN),
                Arguments.of(AuthErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(AuthErrorCode.PASSWORD_CHECK_LIMITED, HttpStatus.TOO_MANY_REQUESTS));
    }

    @ParameterizedTest(name = "{0} is {1}")
    @MethodSource("everyCode")
    @DisplayName("Every code of openapi Problem.code maps to its status as application/problem+json")
    void everyCodeMapsToItsStatus(AuthErrorCode code, HttpStatus expected) throws Exception {
        mvc.perform(get("/api/v1/auth-probe/code/" + code.name()))
                .andExpect(status().is(expected.value()))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.status").value(expected.value()));
    }

    @Test
    void theEnumHasExactlyTheCodesOfTheContract() {
        assertThat(AuthErrorCode.values()).hasSize(23);
    }

    @Test
    @DisplayName("FR-012: ACCOUNT_LOCKED carries retryAt")
    void lockedCarriesRetryAt() throws Exception {
        mvc.perform(get("/api/v1/auth-probe/locked")).andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.retryAt").value(WHEN.toString()));
    }

    @Test
    @DisplayName("FR-013: ACCOUNT_SUSPENDED carries reason and endsAt, ACCOUNT_BANNED reason and closureToken")
    void restrictedAccountsCarryTheirDetails() throws Exception {
        mvc.perform(get("/api/v1/auth-probe/suspended")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
                .andExpect(jsonPath("$.reason").value("Spam"))
                .andExpect(jsonPath("$.endsAt").value(WHEN.toString()));
        mvc.perform(get("/api/v1/auth-probe/banned")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BANNED"))
                .andExpect(jsonPath("$.reason").value("Fraud"))
                .andExpect(jsonPath("$.closureToken").value("closure-token-123"))
                .andExpect(jsonPath("$.endsAt").doesNotExist());
    }

    @Test
    @DisplayName("FR-017: EMAIL_RATE_LIMITED carries retryAfterSeconds and a Retry-After header")
    void rateLimitedCarriesRetryAfter() throws Exception {
        mvc.perform(get("/api/v1/auth-probe/email-rate-limited")).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(900));
    }

    @Test
    @DisplayName("NFR-007: VALIDATION_ERROR lists errors[] with field and code")
    void validationErrorListsFieldCodes() throws Exception {
        mvc.perform(post("/api/v1/auth-probe/form").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\" \",\"email\":\"not-an-email\",\"nickname\":\"toolong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field=='displayName')].code").value("REQUIRED"))
                .andExpect(jsonPath("$.errors[?(@.field=='email')].code").value("INVALID_EMAIL"))
                .andExpect(jsonPath("$.errors[?(@.field=='nickname')].code").value("TOO_LONG"));
    }

    @Test
    @DisplayName("A body that is not JSON is VALIDATION_ERROR too")
    void malformedJsonIsValidationError() throws Exception {
        mvc.perform(post("/api/v1/auth-probe/form").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Constitution IV: no stack trace, exception message or class name reaches the client")
    void unexpectedErrorsDoNotLeak() throws Exception {
        String body = mvc.perform(get("/api/v1/auth-probe/crash")).andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("secret internal detail").doesNotContain("com.wikicook")
                .doesNotContain("Exception").doesNotContain("at java.");
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("R15: AccessDeniedException from a controller outside the auth package is 403 ACCESS_DENIED, not 500")
    void accessDeniedFromAForeignModuleIs403() throws Exception {
        mvc.perform(get("/api/v1/foreign/denied")).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBER")
    @DisplayName("FR-006: @VerifiedEmailRequired and no EMAIL_VERIFIED authority is 403 EMAIL_NOT_VERIFIED")
    void unverifiedUserOnAVerifiedOnlyHandlerIsEmailNotVerified() throws Exception {
        mvc.perform(get("/api/v1/foreign/verified-only")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEMBER", "EMAIL_VERIFIED"})
    @DisplayName("A verified user denied on a verified-only handler is ACCESS_DENIED, not EMAIL_NOT_VERIFIED")
    void verifiedUserDeniedForAnotherReasonIsAccessDenied() throws Exception {
        mvc.perform(get("/api/v1/foreign/verified-only")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("R15: a validation error in a 030 controller is still answered by MealPlanExceptionHandler, old format")
    void mealPlanValidationErrorKeepsItsOwnFormat() throws Exception {
        mvc.perform(post("/api/v1/meal-probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("The request is not valid"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
