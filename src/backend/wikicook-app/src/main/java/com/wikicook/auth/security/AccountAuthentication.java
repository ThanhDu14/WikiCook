package com.wikicook.auth.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** An authenticated request: the principal is the {@link AuthenticatedAccount}, the credentials the access token. */
public class AccountAuthentication extends AbstractAuthenticationToken {

    private final AuthenticatedAccount account;
    private final Jwt token;

    public AccountAuthentication(AuthenticatedAccount account, Collection<? extends GrantedAuthority> authorities) {
        this(account, authorities, null);
    }

    public AccountAuthentication(AuthenticatedAccount account, Collection<? extends GrantedAuthority> authorities,
            Jwt token) {
        super(authorities);
        this.account = account;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedAccount getPrincipal() {
        return account;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }

    @Override
    public String getName() {
        return Long.toString(account.userId());
    }
}
