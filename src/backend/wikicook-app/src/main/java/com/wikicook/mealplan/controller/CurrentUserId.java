package com.wikicook.mealplan.controller;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.UnauthenticatedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The caller's {@code users.id}: from spec 010's {@link CurrentUser} (or the dev stubs). Without one every call is
 * a 401 {@code UNAUTHENTICATED}, as in 031 (T024).
 */
@Component
class CurrentUserId {

    private final ObjectProvider<CurrentUser> currentUser;

    CurrentUserId(ObjectProvider<CurrentUser> currentUser) {
        this.currentUser = currentUser;
    }

    long get() {
        CurrentUser user = currentUser.getIfAvailable();
        if (user == null) {
            throw new UnauthenticatedException();
        }
        return user.id();
    }
}
