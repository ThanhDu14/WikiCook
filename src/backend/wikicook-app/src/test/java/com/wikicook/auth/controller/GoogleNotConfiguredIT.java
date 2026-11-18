package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spec 010 NFR-005 (T143), Google <b>not configured</b> (no client id, as locally and in CI): the application starts
 * without any call to Google, the start of the flow answers with the same address as a Google failure, and e-mail
 * sign-in is not affected. The test context sets no Google property, so it is the context a developer gets by default.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class GoogleNotConfiguredIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        support.resetThrottle();
    }

    @Test
    @DisplayName("no client id: the application started, and there is no Google registration at all")
    void nothingIsBuilt() {
        assertThat(context.getBeanNamesForType(ClientRegistrationRepository.class)).isEmpty();
    }

    @Test
    @DisplayName("GET /auth/oauth2/google goes back to /login?error=google_unavailable, with the redirect ignored")
    void startGoesBackToLogin() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/v1/auth/oauth2/google").param("redirect", "/meal-planner"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("http://localhost:3000/login?error=google_unavailable");
        assertThat(response.getCookie("wc_oauth")).isNull();
    }

    @Test
    @DisplayName("a callback that nobody asked for is no sign-in: no session, no cookie")
    void callbackIsNotAnEndpoint() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/v1/auth/oauth2/callback/google").param("code", "x").param("state", "y"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isNotEqualTo(200);
        assertThat(response.getHeaders("Set-Cookie")).noneMatch(h -> h.startsWith("wc_rt="));
    }

    @Test
    @DisplayName("POST /auth/login still works")
    void passwordSignInWorks() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, "Correct-horse-1");

        MockHttpServletResponse login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"Correct-horse-1\",\"rememberMe\":false}".formatted(email)))
                .andReturn().getResponse();

        assertThat(login.getStatus()).isEqualTo(200);
    }
}
