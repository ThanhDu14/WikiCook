package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * NFR-003: a wrong password, an unknown email and an account without a password take the same time to answer, so the
 * response time does not tell whether an email is registered. BCrypt cost 10 makes the comparison dominate; the unknown
 * and the passwordless account compare against the same dummy hash. Medians over 50 samples each, within 20%.
 */
@SpringBootTest(properties = "wikicook.auth.bcrypt-strength=10")
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class SignInTimingIT extends AbstractPostgresIT {

    private static final int SAMPLES = 50;
    private static final String PASSWORD = "abc12345";

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    private long timeOf(String email, String password) throws Exception {
        long start = System.nanoTime();
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
        return System.nanoTime() - start;
    }

    private static double median(List<Long> samples) {
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static void assertWithin20Percent(String what, double a, double b) {
        assertThat(Math.abs(a - b) / Math.max(a, b)).as("%s: %.1f ms vs %.1f ms", what, a / 1e6, b / 1e6)
                .isLessThanOrEqualTo(0.20);
    }

    @Test
    @DisplayName("NFR-003: wrong password, unknown email and account without a password answer in the same time")
    void responseTimeDoesNotRevealWhetherTheEmailIsRegistered() throws Exception {
        List<String> registered = new ArrayList<>();
        List<String> passwordless = new ArrayList<>();
        for (int i = 0; i < SAMPLES + 3; i++) {
            String email = AuthTestSupport.uniqueEmail();
            support.memberWithPassword(email, PASSWORD);
            registered.add(email);
            String google = AuthTestSupport.uniqueEmail();
            support.userWithPassword(google, null, Role.MEMBER, AccountStatus.ACTIVE, true);
            passwordless.add(google);
        }
        for (int i = 0; i < 3; i++) { // warm up the JIT and the connection pool
            timeOf(registered.get(SAMPLES + i), "wrong-password1");
            timeOf(AuthTestSupport.uniqueEmail(), "wrong-password1");
            timeOf(passwordless.get(SAMPLES + i), "wrong-password1");
        }
        support.resetThrottle();

        List<Long> wrong = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            wrong.add(timeOf(registered.get(i), "wrong-password1"));
        }
        support.resetThrottle();
        List<Long> unknown = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            unknown.add(timeOf(AuthTestSupport.uniqueEmail(), "wrong-password1"));
        }
        support.resetThrottle();
        List<Long> noPassword = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            noPassword.add(timeOf(passwordless.get(i), "wrong-password1"));
        }

        assertWithin20Percent("wrong password vs unknown email", median(wrong), median(unknown));
        assertWithin20Percent("wrong password vs no password", median(wrong), median(noPassword));
        assertWithin20Percent("unknown email vs no password", median(unknown), median(noPassword));
    }
}
