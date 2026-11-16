package com.wikicook.common.security;

/**
 * {@link CurrentUser} plus what spec 010 knows about the caller. {@code id()} keeps its meaning, so modules that
 * only need the id (030, 031, 040) are not touched; modules that need the role or the verification state inject
 * this type (FR-020).
 */
public interface CurrentAccount extends CurrentUser {

    Role role();

    boolean emailVerified();

    /** {@code user_sessions.id} of the request's access token. */
    long sessionId();
}
