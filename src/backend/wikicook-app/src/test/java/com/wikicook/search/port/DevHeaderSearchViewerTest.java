package com.wikicook.search.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** No header means guest, never the default dev user (020 research R6, acceptance 3.4). */
class DevHeaderSearchViewerTest {

    private final DevHeaderSearchViewer viewer = new DevHeaderSearchViewer();

    @AfterEach
    void reset() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void request(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) {
            request.addHeader(DevHeaderSearchViewer.HEADER, header);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void headerGivesTheUser() {
        request(" 7 ");

        assertThat(viewer.currentUserId()).contains(7L);
    }

    @Test
    void noHeaderIsGuest() {
        request(null);

        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void blankHeaderIsGuest() {
        request("  ");

        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void outsideARequestIsGuest() {
        assertThat(viewer.currentUserId()).isEmpty();
    }

    @Test
    void malformedHeaderIsA400() {
        request("abc");

        assertThatThrownBy(viewer::currentUserId).isInstanceOf(ErrorResponseException.class)
                .satisfies(e -> assertThat(((ErrorResponseException) e).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void zeroOrNegativeIsA400() {
        request("0");

        assertThatThrownBy(viewer::currentUserId).isInstanceOf(ErrorResponseException.class);
    }
}
