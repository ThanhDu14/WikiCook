package com.wikicook.devstub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Dev user from {@code X-Dev-User-Id}, else the configured id; the user row is provisioned on first use. */
class StubCurrentUserTest {

    private final DevUserProvisioner provisioner = mock(DevUserProvisioner.class);
    private final StubCurrentUser currentUser = new StubCurrentUser(5, provisioner);

    private static void request(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) {
            request.addHeader(StubCurrentUser.HEADER, header);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void reset() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void headerWins() {
        request("12");
        assertThat(currentUser.id()).isEqualTo(12);
        verify(provisioner).ensure(12);
    }

    @Test
    void withoutHeaderUsesConfiguredId() {
        request(null);
        assertThat(currentUser.id()).isEqualTo(5);
        verify(provisioner).ensure(5);
    }

    @Test
    void outsideARequestUsesConfiguredId() {
        assertThat(currentUser.id()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-3", "1.5", " ", "99999999999999999999"})
    void invalidHeaderIsA400ValidationError(String header) {
        request(header);
        assertThatThrownBy(currentUser::id)
                .isInstanceOfSatisfying(ErrorResponseException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getBody().getProperties()).containsEntry("code", "VALIDATION_ERROR");
                });
    }
}
