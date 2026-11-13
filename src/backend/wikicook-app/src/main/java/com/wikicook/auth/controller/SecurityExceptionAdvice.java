package com.wikicook.auth.controller;

import com.wikicook.auth.service.AuthException;
import com.wikicook.common.security.VerifiedEmailRequired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

/**
 * Global advice for one exception only: {@link AccessDeniedException}, which method security
 * ({@code @ModeratorOnly}, {@code @AdministratorOnly}, {@code @VerifiedEmailRequired}) throws <em>inside</em> a
 * controller of any module, so it never reaches the security filters. Without this, {@code GlobalExceptionHandler}
 * would turn every missing permission into a 500 (research R15). It is 403 {@code EMAIL_NOT_VERIFIED} when the handler
 * carries {@code @VerifiedEmailRequired} and the caller lacks {@code EMAIL_VERIFIED}, otherwise 403
 * {@code ACCESS_DENIED}. It deliberately handles nothing else: the advices of 030, 031 and 040 own validation errors
 * of their controllers.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionAdvice {

    private static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handle(AccessDeniedException exception,
            HandlerMethod handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthException problem;
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            problem = AuthException.unauthenticated();
        } else if (requiresVerifiedEmail(handler) && !hasAuthority(authentication)) {
            problem = AuthException.emailNotVerified();
        } else {
            problem = AuthException.accessDenied();
        }
        return ResponseEntity.status(problem.code().status()).body(problem.problem());
    }

    private static boolean requiresVerifiedEmail(HandlerMethod handler) {
        return handler != null
                && (AnnotatedElementUtils.hasAnnotation(handler.getMethod(), VerifiedEmailRequired.class)
                        || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), VerifiedEmailRequired.class));
    }

    private static boolean hasAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> EMAIL_VERIFIED.equals(a.getAuthority()));
    }
}
