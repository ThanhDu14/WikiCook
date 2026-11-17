package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.RecordingMailSender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NFR-003: asking for a reset link for a registered address, an unknown one and one that must get no link (Google-only)
 * is answered with the same status and body, in the same time: looking the account up, issuing the token and sending
 * the mail all run in the background, so the request does only the limit check. Medians over 50 samples each, within
 * 20%. Each sample uses its own address, so the limit of three per address never interferes. A short pause after every
 * request lets the background task of that request finish before the next one is timed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, RecordingMailSender.class})
class ForgotPasswordTimingIT extends AbstractPostgresIT {

    private static final int SAMPLES = 50;
    private static final int WARM_UP = 5;
    private static final String PASSWORD = "abc12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
        RecordingMailSender.recorder().clear();
    }

    private MockHttpServletResponse ask(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password/forgot").contentType("application/json")
                .content("{\"email\":\"" + email + "\"}")).andReturn().getResponse();
    }

    private long timeOf(String email) throws Exception {
        long start = System.nanoTime();
        ask(email);
        long elapsed = System.nanoTime() - start;
        Thread.sleep(30); // let this request's background task finish before the next sample
        return elapsed;
    }

    private static double median(List<Long> samples) {
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static void assertWithin20Percent(String what, double a, double b) {
        assertThat(Math.abs(a - b) / Math.max(a, b)).as("%s: %.2f ms vs %.2f ms", what, a / 1e6, b / 1e6)
                .isLessThanOrEqualTo(0.20);
    }

    @Test
    @DisplayName("NFR-003: registered, unknown and Google-only addresses answer the same and in the same time")
    void responseDoesNotRevealWhetherTheEmailIsRegistered() throws Exception {
        List<String> registered = new ArrayList<>();
        List<String> googleOnly = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (int i = 0; i < SAMPLES + WARM_UP; i++) {
            String email = AuthTestSupport.uniqueEmail();
            support.memberWithPassword(email, PASSWORD);
            registered.add(email);
            String google = AuthTestSupport.uniqueEmail();
            support.userWithPassword(google, null, Role.MEMBER, AccountStatus.ACTIVE, true);
            googleOnly.add(google);
            unknown.add(AuthTestSupport.uniqueEmail());
        }

        MockHttpServletResponse known = ask(registered.get(0));
        MockHttpServletResponse stranger = ask(unknown.get(0));
        MockHttpServletResponse passwordless = ask(googleOnly.get(0));
        assertThat(known.getStatus()).isEqualTo(202);
        assertThat(stranger.getStatus()).isEqualTo(known.getStatus());
        assertThat(passwordless.getStatus()).isEqualTo(known.getStatus());
        assertThat(stranger.getContentAsString()).isEqualTo(known.getContentAsString());
        assertThat(passwordless.getContentAsString()).isEqualTo(known.getContentAsString());

        for (int i = 1; i < WARM_UP; i++) { // warm up the JIT, the pool and the mail renderer
            timeOf(registered.get(i));
            timeOf(unknown.get(i));
            timeOf(googleOnly.get(i));
        }

        List<Long> registeredTimes = new ArrayList<>();
        List<Long> unknownTimes = new ArrayList<>();
        List<Long> googleTimes = new ArrayList<>();
        for (int i = WARM_UP; i < WARM_UP + SAMPLES; i++) { // interleaved, so a slow moment hits all three alike
            registeredTimes.add(timeOf(registered.get(i)));
            unknownTimes.add(timeOf(unknown.get(i)));
            googleTimes.add(timeOf(googleOnly.get(i)));
        }

        assertWithin20Percent("registered vs unknown", median(registeredTimes), median(unknownTimes));
        assertWithin20Percent("registered vs Google-only", median(registeredTimes), median(googleTimes));
        assertWithin20Percent("unknown vs Google-only", median(unknownTimes), median(googleTimes));
    }
}
