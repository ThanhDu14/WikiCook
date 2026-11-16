package com.wikicook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.auth.service.AccessTokenService;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import com.wikicook.support.MatrixProbeController;
import com.wikicook.support.TestAuthProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Research R10: the URL layer denies by default and opens only the declared public routes. Real filter chain and real
 * JWT. {@code spring.mail.host} points at a host that does not exist to prove an SMTP outage cannot take the health
 * endpoint down (research R8).
 */
@SpringBootTest(properties = "spring.mail.host=no-such-smtp-host.invalid")
@AutoConfigureMockMvc
@Import({AuthTestSupport.class, MatrixProbeController.class})
class SecurityFilterChainIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @Value("${wikicook.auth.jwt-secret}")
    String jwtSecret;

    @Test
    @DisplayName("US3-1: no token on a protected route is 401 UNAUTHENTICATED, written by the filter as a ProblemDetail")
    void noTokenIs401Problem() throws Exception {
        mvc.perform(get("/api/v1/probe/whoami")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("NFR-005: the health endpoint and the OpenAPI document are open")
    void healthAndOpenApiAreOpen() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("R8: health stays UP when the SMTP host does not exist")
    void healthIsUpWhenSmtpIsUnreachable() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("US3-1 / 020 AS 3.4: a guest searches recipes without a token (200, not 401)")
    void guestMaySearchRecipes() throws Exception {
        mvc.perform(get("/api/v1/recipes/search")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recipes/search/options")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recipes/featured")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("R10: a route outside /api that nobody declared is refused")
    void undeclaredRouteOutsideApiIsRefused() throws Exception {
        mvc.perform(get("/something-else")).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("R10: an unknown /api/v1 route is 401 without a token and 404 ProblemDetail with a real JWT")
    void unknownApiRouteIs401ThenNotFound() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        SignedIn caller = support.signedInMember();
        mvc.perform(get("/api/v1/does-not-exist").header("Authorization", caller.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    @DisplayName("Architecture: stateless, no HttpSession is created and no session cookie is set")
    void noHttpSessionIsCreated() throws Exception {
        SignedIn caller = support.signedInMember();

        MvcResult result = mvc.perform(get("/api/v1/probe/whoami").header("Authorization", caller.bearer()))
                .andExpect(status().isOk()).andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeaders("Set-Cookie")).noneMatch(c -> c.contains("JSESSIONID"));
    }

    @Test
    @DisplayName("R2: refresh and logout need X-WikiCook-Client: web, else 403 INVALID_CLIENT")
    void cookieEndpointsRequireTheClientHeader() throws Exception {
        for (String path : new String[] {"/api/v1/auth/refresh", "/api/v1/auth/logout"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("INVALID_CLIENT"));
            mvc.perform(post(path).header("X-WikiCook-Client", "mobile")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("INVALID_CLIENT"));
        }
    }

    @Test
    @DisplayName("R10: a forged or expired JWT sent to /auth/refresh or /auth/login does not make them 401")
    void badBearerIsIgnoredOnPublicRoutes() throws Exception {
        AccessTokenService yesterday = new AccessTokenService(TestAuthProperties.create(jwtSecret, "key"),
                Clock.fixed(Instant.now().minusSeconds(24 * 3600), ZoneOffset.UTC));
        String expired = "Bearer " + yesterday.issue(1L, 1L).value();
        String forged = "Bearer " + new AccessTokenService(TestAuthProperties.create(
                "another-secret-0123456789abcdef0123456789abcdef", "key"), Clock.systemUTC()).issue(1L, 1L).value();

        String email = AuthTestSupport.uniqueEmail();
        support.memberWithPassword(email, "abc12345");
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"abc12345\"}";

        for (String bearer : new String[] {expired, forged, "Bearer garbage"}) {
            // real sign-in: the bearer must be ignored, the answer is 200 and not 401
            MvcResult login = mvc.perform(post("/api/v1/auth/login").header("Authorization", bearer)
                    .contentType("application/json").content(credentials)).andReturn();
            assertThat(login.getResponse().getStatus()).as("login with %s", bearer).isEqualTo(200);
            String cookie = login.getResponse().getHeader("Set-Cookie").split(";")[0].substring("wc_rt=".length());
            MvcResult refresh = mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web")
                    .header("Authorization", bearer).cookie(new jakarta.servlet.http.Cookie("wc_rt", cookie)))
                    .andReturn();
            assertThat(refresh.getResponse().getStatus()).as("refresh with %s", bearer).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("R10: the same bad bearer on a protected route is 401")
    void badBearerOnAProtectedRouteIs401() throws Exception {
        mvc.perform(get("/api/v1/probe/whoami").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("US3-2, US3-3, FR-021: roles inherit downwards, a Contributor does not reach moderator or admin routes")
    void roleHierarchyAppliesToMethodSecurity() throws Exception {
        SignedIn member = support.signedIn(com.wikicook.common.security.Role.MEMBER,
                com.wikicook.common.security.AccountStatus.ACTIVE, true);
        SignedIn contributor = support.signedIn(com.wikicook.common.security.Role.CONTRIBUTOR,
                com.wikicook.common.security.AccountStatus.ACTIVE, true);
        SignedIn moderator = support.signedIn(com.wikicook.common.security.Role.MODERATOR,
                com.wikicook.common.security.AccountStatus.ACTIVE, true);
        SignedIn administrator = support.signedIn(com.wikicook.common.security.Role.ADMINISTRATOR,
                com.wikicook.common.security.AccountStatus.ACTIVE, true);

        for (SignedIn lower : new SignedIn[] {member, contributor}) {
            mvc.perform(get("/api/v1/probe/moderator").header("Authorization", lower.bearer()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            mvc.perform(get("/api/v1/probe/administrator").header("Authorization", lower.bearer()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        mvc.perform(get("/api/v1/probe/moderator").header("Authorization", moderator.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/probe/administrator").header("Authorization", moderator.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/probe/moderator").header("Authorization", administrator.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/probe/administrator").header("Authorization", administrator.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-006: @VerifiedEmailRequired lets a verified user in and gives an unverified one EMAIL_NOT_VERIFIED")
    void verifiedEmailRequiredEndToEnd() throws Exception {
        SignedIn verified = support.signedIn(com.wikicook.common.security.Role.MEMBER,
                com.wikicook.common.security.AccountStatus.ACTIVE, true);
        SignedIn unverified = support.signedIn(com.wikicook.common.security.Role.MEMBER,
                com.wikicook.common.security.AccountStatus.ACTIVE, false);

        mvc.perform(get("/api/v1/probe/verified").header("Authorization", verified.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/probe/verified").header("Authorization", unverified.bearer()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("NFR-009: API responses are not cached")
    void apiResponsesAreNoStore() throws Exception {
        SignedIn caller = support.signedInMember();

        mvc.perform(get("/api/v1/probe/whoami").header("Authorization", caller.bearer()))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(get("/api/v1/probe/whoami"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }
}
