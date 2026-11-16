package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.common.security.Role;
import com.wikicook.common.security.UnauthenticatedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/** FR-020: the id, role, verification state and session of the caller come from the authenticated principal. */
class SecurityContextCurrentAccountTest {

    private final SecurityContextCurrentAccount current = new SecurityContextCurrentAccount();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(AuthenticatedAccount account) {
        SecurityContextHolder.getContext().setAuthentication(new AccountAuthentication(account,
                AuthorityUtils.createAuthorityList("ROLE_" + account.role().name())));
    }

    @Test
    void readsIdRoleVerificationAndSessionFromThePrincipal() {
        signIn(new AuthenticatedAccount(42L, Role.MODERATOR, true, 7L, null));

        assertThat(current.id()).isEqualTo(42L);
        assertThat(current.role()).isEqualTo(Role.MODERATOR);
        assertThat(current.emailVerified()).isTrue();
        assertThat(current.sessionId()).isEqualTo(7L);
    }

    @Test
    void anUnverifiedAccountIsReportedAsUnverified() {
        signIn(new AuthenticatedAccount(5L, Role.MEMBER, false, 1L, null));

        assertThat(current.emailVerified()).isFalse();
    }

    @Test
    void nobodySignedInIsAnUnauthenticatedException() {
        assertThatThrownBy(current::id).isInstanceOf(UnauthenticatedException.class)
                .satisfies(e -> {
                    var problem = ((UnauthenticatedException) e).getBody();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                    assertThat(problem.getProperties()).containsEntry("code", "UNAUTHENTICATED");
                });
        assertThatThrownBy(current::role).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(current::emailVerified).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(current::sessionId).isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void anAnonymousGuestIsAnUnauthenticatedException() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(() -> "ROLE_ANONYMOUS")));

        assertThatThrownBy(current::id).isInstanceOf(UnauthenticatedException.class);
    }
}
