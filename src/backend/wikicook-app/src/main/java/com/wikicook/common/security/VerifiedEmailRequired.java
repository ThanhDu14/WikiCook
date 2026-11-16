package com.wikicook.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The caller must have a verified email (FR-006): submitting a recipe, reviewing, rating, cooksnap. Without it the
 * answer is 403 {@code EMAIL_NOT_VERIFIED}. Put it on the controller method or class of the module that needs it.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAuthority('EMAIL_VERIFIED')")
public @interface VerifiedEmailRequired {
}
