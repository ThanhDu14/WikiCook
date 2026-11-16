package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.common.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 020 acceptance 3.4 (research R6, R13): a guest sees unfiltered results, a signed-in user is identified only by the
 * authenticated principal; the dev and test headers have no effect any more.
 */
class AuthSearchViewerTest {

    private final AuthSearchViewer viewer = new AuthSearchViewer();
    private MockHttpServletRequest request;

    @BeforeEach
    void bindRequest() {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void noAuthenticationIsAGuest() {
        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void anonymousAuthenticationIsAGuest() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void aSignedInUserIsTheirOwnId() {
        SecurityContextHolder.getContext().setAuthentication(new AccountAuthentication(
                new AuthenticatedAccount(42L, Role.MEMBER, true, 7L, null),
                AuthorityUtils.createAuthorityList("ROLE_MEMBER")));

        assertThat(viewer.currentUserId()).contains(42L);
    }

    @Test
    void devAndTestHeadersDoNothingForAGuest() {
        request.addHeader("X-Dev-User-Id", "5");
        request.addHeader("X-Test-User", "6");
        request.addHeader("X-User-Id", "7");

        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void headersCannotChangeWhoTheSignedInUserIs() {
        request.addHeader("X-Dev-User-Id", "5");
        SecurityContextHolder.getContext().setAuthentication(new AccountAuthentication(
                new AuthenticatedAccount(42L, Role.MEMBER, true, 7L, null),
                AuthorityUtils.createAuthorityList("ROLE_MEMBER")));

        assertThat(viewer.currentUserId()).contains(42L);
    }

    @Test
    void worksWithoutAnyRequestBoundToTheThread() {
        RequestContextHolder.resetRequestAttributes();

        assertThat(viewer.currentUserId()).isEmpty();
    }
}
