package com.wikicook.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.service.AuthException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Writes an {@link AuthException} as {@code application/problem+json}. Errors raised inside security filters never
 * reach a {@code @RestControllerAdvice}, so the entry point, the access-denied handler and {@code ClientHeaderFilter}
 * all go through here (research R15).
 */
@Component
public class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, AuthException exception) throws IOException {
        ProblemDetail problem = exception.problem();
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Long retryAfter = exception.retryAfterSeconds();
        if (retryAfter != null) {
            response.setHeader("Retry-After", Long.toString(retryAfter));
        }
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
