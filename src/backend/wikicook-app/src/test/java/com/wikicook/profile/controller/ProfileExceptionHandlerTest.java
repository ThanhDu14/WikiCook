package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.wikicook.profile.service.ProfileErrorCode;
import com.wikicook.profile.service.ProfileException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** T010 (research R17): every code answers with its HTTP status, field errors keep their field, nothing leaks. */
class ProfileExceptionHandlerTest {

    /** The table of research R17, except the codes of 010 and PROFILE_UNAVAILABLE (T034a). */
    private static final Map<String, HttpStatus> R17 = Map.ofEntries(
            Map.entry("VALIDATION_ERROR", HttpStatus.BAD_REQUEST),
            Map.entry("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("PROFILE_CHANGED", HttpStatus.CONFLICT),
            Map.entry("PASSWORD_NOT_SET", HttpStatus.CONFLICT),
            Map.entry("PASSWORD_ALREADY_SET", HttpStatus.CONFLICT),
            Map.entry("LAST_ADMINISTRATOR", HttpStatus.CONFLICT),
            Map.entry("ACCOUNT_STATE_CHANGED", HttpStatus.CONFLICT),
            Map.entry("TOKEN_INVALID", HttpStatus.GONE),
            Map.entry("AVATAR_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE),
            Map.entry("AVATAR_TYPE_NOT_ALLOWED", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
            Map.entry("AVATAR_NOT_AN_IMAGE", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("AVATAR_DIMENSIONS_TOO_LARGE", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("INVALID_CURRENT_PASSWORD", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("PASSWORD_SAME_AS_CURRENT", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("CONFIRMATION_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("PROFILE_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS),
            Map.entry("PASSWORD_CHECK_LIMITED", HttpStatus.TOO_MANY_REQUESTS));

    private final ProfileExceptionHandler handler = new ProfileExceptionHandler();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(ResponseEntity<ProblemDetail> response) {
        return response.getBody().getProperties();
    }

    @Test
    @DisplayName("R17: the enum has exactly the codes of the table, with their status")
    void everyCodeHasItsStatus() {
        Set<String> codes = java.util.Arrays.stream(ProfileErrorCode.values()).map(Enum::name)
                .collect(Collectors.toSet());
        assertThat(codes).isEqualTo(R17.keySet());
        R17.forEach((code, status) -> {
            ResponseEntity<ProblemDetail> response = handler.handle(new ProfileException(ProfileErrorCode.valueOf(code)));
            assertThat(response.getStatusCode()).as(code).isEqualTo(status);
            assertThat(properties(response)).as(code).containsEntry("code", code);
            assertThat(response.getBody().getStatus()).as(code).isEqualTo(status.value());
        });
    }

    @Test
    @DisplayName("NFR-008: a rate-limited answer carries retryAfterSeconds and Retry-After")
    void rateLimitedHasRetryAfter() {
        ResponseEntity<ProblemDetail> response = handler.handle(ProfileException.rateLimited(754));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(properties(response)).containsEntry("retryAfterSeconds", 754);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("754");
    }

    @Test
    @DisplayName("FR-016: errors[] lists field and code")
    void fieldErrorsAreListed() {
        ResponseEntity<ProblemDetail> response = handler.handle(ProfileException.invalid(
                List.of(new ProfileException.FieldCode("allergenIds", "UNKNOWN_ALLERGEN"))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(properties(response)).containsEntry("code", "VALIDATION_ERROR");
        assertThat(properties(response).get("errors"))
                .isEqualTo(List.of(new ProfileException.FieldCode("allergenIds", "UNKNOWN_ALLERGEN")));
    }

    @Test
    @DisplayName("Bean Validation: the message of the constraint is the code, the property is the field")
    void beanValidationErrorsBecomeFieldCodes() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "householdSize", 21, false, new String[] {"Max"}, null, "OUT_OF_RANGE"));
        binding.addError(new FieldError("request", "excludedIngredientIds", null, false, new String[] {"Size"}, null,
                "EXCLUDED_LIMIT_EXCEEDED"));
        binding.addError(new FieldError("request", "dietType", null, false, new String[] {"NotNull"}, null,
                "must not be null"));
        Method method = ProfileExceptionHandlerTest.class.getDeclaredMethod("everyCodeHasItsStatus");

        ResponseEntity<ProblemDetail> response = handler.handle(
                new MethodArgumentNotValidException(new MethodParameter(method, -1), binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(properties(response).get("errors")).isEqualTo(List.of(
                new ProfileException.FieldCode("householdSize", "OUT_OF_RANGE"),
                new ProfileException.FieldCode("excludedIngredientIds", "EXCLUDED_LIMIT_EXCEEDED"),
                new ProfileException.FieldCode("dietType", "REQUIRED")));
    }

    @Test
    @DisplayName("Edge: Invalid data - householdSize 2.5 (InvalidFormatException) keeps its field")
    void notAWholeNumberKeepsTheField() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not an int", 2.5, Integer.class);
        cause.prependPath(new Object(), "householdSize");

        ResponseEntity<ProblemDetail> response = handler.handle(
                new HttpMessageNotReadableException("bad body", cause, new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(properties(response)).containsEntry("code", "VALIDATION_ERROR");
        assertThat(properties(response).get("errors"))
                .isEqualTo(List.of(new ProfileException.FieldCode("householdSize", "NOT_A_WHOLE_NUMBER")));
    }

    @Test
    @DisplayName("Edge: Invalid data - an unreadable body is 400 without a field and without the parser message")
    void unreadableBodyIsPlain400() {
        MismatchedInputException cause = MismatchedInputException.from(null, Object.class, "Unexpected token at line 1");

        ResponseEntity<ProblemDetail> response = handler.handle(
                new HttpMessageNotReadableException("secret parser detail", cause, new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(properties(response)).containsEntry("code", "VALIDATION_ERROR").doesNotContainKey("errors");
        assertThat(response.getBody().getDetail()).doesNotContain("secret").doesNotContain("Unexpected");
    }

    @Test
    @DisplayName("R7: a request over the multipart limit is 413 AVATAR_TOO_LARGE")
    void tooLargeUploadIs413() {
        ResponseEntity<ProblemDetail> response = handler.handle(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(properties(response)).containsEntry("code", "AVATAR_TOO_LARGE");
    }

    @Test
    @DisplayName("R7: a multipart read that failed on the size limit of the container is 413, any other multipart failure is 400")
    void multipartFailuresAreTold() {
        ResponseEntity<ProblemDetail> tooLarge = handler.handle(new org.springframework.web.multipart.MultipartException(
                "Could not access multipart servlet request", new MaxUploadSizeExceededException(5L * 1024 * 1024)));
        ResponseEntity<ProblemDetail> broken = handler.handle(new org.springframework.web.multipart.MultipartException(
                "Could not access multipart servlet request", new IllegalStateException("stream ended")));

        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(properties(tooLarge)).containsEntry("code", "AVATAR_TOO_LARGE");
        assertThat(broken.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(properties(broken)).containsEntry("code", "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Constitution IV: the detail is fixed text, not the exception message")
    void noInternalDetailLeaks() {
        ResponseEntity<ProblemDetail> response = handler.handle(new ProfileException(ProfileErrorCode.USER_NOT_FOUND));

        assertThat(response.getBody().getDetail()).doesNotContain("Exception").doesNotContain("com.wikicook");
    }
}
