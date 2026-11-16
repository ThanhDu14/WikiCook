package com.wikicook.auth.security;

import com.wikicook.common.security.CurrentAccount;
import com.wikicook.common.security.CurrentUser;
import com.wikicook.common.security.Role;
import com.wikicook.common.security.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The real {@link CurrentUser} (and {@link CurrentAccount}) of the application: reads the principal that
 * {@code SessionJwtAuthenticationConverter} put in the security context. As an application bean it makes the dev stub
 * of {@code DevStubConfig} back off ({@code @ConditionalOnMissingBean}); 030, 031 and 040 keep injecting
 * {@code CurrentUser} and now get a caller that cannot be impersonated with a header.
 */
@Component
public class SecurityContextCurrentAccount implements CurrentAccount {

    @Override
    public long id() {
        return principal().userId();
    }

    @Override
    public Role role() {
        return principal().role();
    }

    @Override
    public boolean emailVerified() {
        return principal().emailVerified();
    }

    @Override
    public long sessionId() {
        return principal().sessionId();
    }

    private static AuthenticatedAccount principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccountAuthentication account) {
            return account.getPrincipal();
        }
        throw new UnauthenticatedException();
    }
}
