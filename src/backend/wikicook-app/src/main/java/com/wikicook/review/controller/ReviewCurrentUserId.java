package com.wikicook.review.controller;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.UnauthenticatedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ReviewCurrentUserId {

    private final ObjectProvider<CurrentUser> currentUser;

    public ReviewCurrentUserId(ObjectProvider<CurrentUser> currentUser) {
        this.currentUser = currentUser;
    }

    public long requireId() {
        CurrentUser user = currentUser.getIfAvailable();
        if (user == null) {
            throw new UnauthenticatedException();
        }
        return user.id();
    }

    public Long getOptionalId() {
        CurrentUser user = currentUser.getIfAvailable();
        return user != null ? user.id() : null;
    }
}

