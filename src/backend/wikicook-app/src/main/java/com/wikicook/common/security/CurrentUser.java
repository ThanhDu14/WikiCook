package com.wikicook.common.security;

/**
 * The signed-in user of the current request. Spec 010 will implement it on Spring Security; until then the dev
 * stubs ({@code com.wikicook.devstub}) read it from a header.
 */
public interface CurrentUser {

    /** {@code users.id} of the caller. */
    long id();
}
