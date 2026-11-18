package com.wikicook.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.wikicook.auth.api.AccountResetEvent;
import com.wikicook.common.media.MediaStorage;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.FakeGoogle;
import com.wikicook.support.FakeGoogle.Person;
import com.wikicook.support.GoogleFlow;
import com.wikicook.support.GoogleFlow.Started;
import com.wikicook.support.GoogleIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spec 010 US5-2, FR-019, OQ-010-07 (T142): Google sign-in with the email of an <b>unverified</b> account takes the
 * account over, so whoever pre-loaded it (an allergy profile, a password, a picture) gains nothing, and the real owner
 * of the email starts clean. It is all or nothing: a listener that fails rolls everything back. An unverified account
 * that is suspended or banned is not taken over, or the takeover would delete the sanction (FR-019).
 */
@GoogleIntegrationTest
@Import(GoogleTakeoverIT.ResetRecorder.class)
class GoogleTakeoverIT extends AbstractPostgresIT {

    private static final String PASSWORD = "Correct-horse-1";

    /** Stands in for the listeners of 060 and 080: it sees every {@link AccountResetEvent} and can be told to fail. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ResetRecorder {

        final List<Long> events = new CopyOnWriteArrayList<>();
        volatile boolean fail;

        @EventListener
        void on(AccountResetEvent event) {
            events.add(event.userId());
            if (fail) {
                throw new IllegalStateException("060 could not delete the drafts");
            }
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    ResetRecorder recorder;

    @Autowired
    MediaStorage storage;

    @BeforeEach
    void setUp() {
        FakeGoogle.reset();
        support.resetThrottle();
        recorder.events.clear();
        recorder.fail = false;
    }

    private MockHttpServletResponse signInWithGoogle(Person person) throws Exception {
        Started started = GoogleFlow.start(mvc, null);
        FakeGoogle.tokenEndpointReturns(person.idToken(started.nonce()));
        return GoogleFlow.callback(mvc, started, started.state());
    }

    private String refreshCookie(MockHttpServletResponse response) {
        for (String header : response.getHeaders("Set-Cookie")) {
            Matcher matcher = Pattern.compile("wc_rt=([^;]+)").matcher(header);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    /** An unverified account with a password, a session, an allergy, a meal plan, a shopping list and an avatar. */
    private record Preloaded(long id, String email, String avatarKey) {
    }

    private Preloaded preload() {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.ACTIVE, false);
        String avatarKey = "avatars/" + UUID.randomUUID() + ".jpg";
        storage.store(avatarKey, new byte[] {1, 2, 3});
        jdbc.update("UPDATE users SET bio = 'Bio của kẻ gài sẵn', display_name = 'Kẻ gài sẵn', diet_type = 'VEGAN', "
                + "household_size = 9, avatar_storage_key = ? WHERE id = ?", avatarKey, id);
        jdbc.update("INSERT INTO user_allergens (user_id, allergen_id) VALUES (?, ?)", id, new ProfileFixtures(jdbc).allergen("PEANUT"));
        jdbc.update("INSERT INTO meal_plans (user_id, week_start) VALUES (?, date '2030-01-07')", id);
        jdbc.update("INSERT INTO shopping_lists (user_id, status) VALUES (?, 'ACTIVE')", id);
        support.session(id);
        return new Preloaded(id, email, avatarKey);
    }

    @Test
    @DisplayName("FR-019: the unverified account is deleted and created again: no data, no password, no session, Google name, verified")
    void takeover() throws Exception {
        Preloaded old = preload();

        MockHttpServletResponse response = signInWithGoogle(new Person("google-" + UUID.randomUUID(), old.email(), "Chủ Thật Của Email"));

        assertThat(response.getHeader("Location")).isEqualTo("http://localhost:3000/auth/callback");
        assertThat(refreshCookie(response)).isNotNull();
        assertThat(count("SELECT count(*) FROM users WHERE id = ?", old.id())).as("the old row is gone").isZero();
        Map<String, Object> fresh = jdbc.queryForMap("""
                SELECT id, role, status, display_name, bio, avatar_storage_key, diet_type, household_size, google_sub,
                       password_hash, email_verified_at IS NOT NULL AS verified
                FROM users WHERE email = ?""", old.email());
        assertThat(fresh.get("id")).isNotEqualTo(old.id());
        assertThat(fresh).containsEntry("display_name", "Chủ Thật Của Email").containsEntry("role", "MEMBER")
                .containsEntry("status", "ACTIVE").containsEntry("verified", true);
        assertThat(fresh.get("google_sub")).isNotNull();
        assertThat(fresh.get("password_hash")).as("the old password is disabled").isNull();
        assertThat(fresh.get("bio")).isNull();
        assertThat(fresh.get("avatar_storage_key")).isNull();
        assertThat(fresh.get("household_size")).isNull();
        assertThat(fresh.get("diet_type")).isNotEqualTo("VEGAN");

        // everything that referenced the old id went with it (ON DELETE CASCADE), nothing moved to the new one
        for (String table : List.of("user_allergens", "meal_plans", "shopping_lists", "user_sessions")) {
            assertThat(count("SELECT count(*) FROM " + table + " WHERE user_id IN (?, ?)", old.id(), fresh.get("id")))
                    .as(table).isEqualTo(table.equals("user_sessions") ? 1 : 0); // only the session of this sign-in exists
        }
        assertThat(storage.load(old.avatarKey())).as("011 removes the avatar file after the commit").isEmpty();
        assertThat(recorder.events).containsExactly(old.id());
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'ACCOUNT_RESET' AND target_id = ?", fresh.get("id")))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT details->>'previousUserId' FROM audit_logs WHERE action = 'ACCOUNT_RESET' AND target_id = ?",
                String.class, fresh.get("id"))).isEqualTo(String.valueOf(old.id()));

        // the old password no longer opens the account
        MockHttpServletResponse login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"rememberMe\":false}".formatted(old.email(), PASSWORD)))
                .andReturn().getResponse();
        assertThat(login.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("FR-019: the sessions of the old account end: its refresh token is dead")
    void oldSessionsEnd() throws Exception {
        Preloaded old = preload();
        String oldToken = "old-refresh-token";
        jdbc.update("UPDATE user_sessions SET token_hash = ? WHERE user_id = ?",
                com.wikicook.auth.service.SessionService.hash(oldToken), old.id());

        signInWithGoogle(new Person("google-" + UUID.randomUUID(), old.email(), "Chủ thật"));

        MockHttpServletResponse refresh = mvc.perform(post("/api/v1/auth/refresh").header("X-WikiCook-Client", "web")
                .cookie(new Cookie("wc_rt", oldToken))).andReturn().getResponse();
        assertThat(refresh.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("OQ-010-07: a listener that fails rolls the takeover back; the old account is untouched, no session")
    void listenerFailureRollsBack() throws Exception {
        Preloaded old = preload();
        recorder.fail = true;

        MockHttpServletResponse response = signInWithGoogle(new Person("google-" + UUID.randomUUID(), old.email(), "Chủ thật"));

        assertThat(response.getHeader("Location")).isEqualTo("http://localhost:3000/login?error=google_unavailable");
        assertThat(refreshCookie(response)).isNull();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id, display_name, bio, password_hash IS NOT NULL AS has_password, email_verified_at IS NOT NULL AS verified,
                       google_sub FROM users WHERE email = ?""", old.email());
        assertThat(row).containsEntry("id", old.id()).containsEntry("display_name", "Kẻ gài sẵn")
                .containsEntry("bio", "Bio của kẻ gài sẵn").containsEntry("has_password", true).containsEntry("verified", false);
        assertThat(row.get("google_sub")).isNull();
        assertThat(count("SELECT count(*) FROM user_allergens WHERE user_id = ?", old.id())).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM meal_plans WHERE user_id = ?", old.id())).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM user_sessions WHERE user_id = ?", old.id())).isEqualTo(1);
        assertThat(storage.load(old.avatarKey())).as("the picture of the account that stays is kept").isPresent();
        assertThat(count("SELECT count(*) FROM audit_logs WHERE action = 'ACCOUNT_RESET' AND details->>'previousUserId' = ?",
                String.valueOf(old.id()))).isZero();
    }

    @Test
    @DisplayName("FR-019: an unverified account that is suspended is not taken over: the sanction and the data stay")
    void suspendedUnverifiedIsNotReset() throws Exception {
        Preloaded old = preload();
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", old.id());
        Instant ends = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        support.sanction(old.id(), "Spam", ends);

        MockHttpServletResponse response = signInWithGoogle(new Person("google-" + UUID.randomUUID(), old.email(), "Chủ thật"));

        assertThat(response.getHeader("Location")).startsWith("http://localhost:3000/account-status#code=ACCOUNT_SUSPENDED&reason=Spam&endsAt=");
        assertThat(refreshCookie(response)).isNull();
        assertUntouched(old);
        assertThat(count("SELECT count(*) FROM user_suspensions WHERE user_id = ? AND lifted_at IS NULL", old.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-019: an unverified account that is banned is not taken over either")
    void bannedUnverifiedIsNotReset() throws Exception {
        Preloaded old = preload();
        jdbc.update("UPDATE users SET status = 'BANNED' WHERE id = ?", old.id());
        support.sanction(old.id(), "Vi phạm", null);

        MockHttpServletResponse response = signInWithGoogle(new Person("google-" + UUID.randomUUID(), old.email(), "Chủ thật"));

        assertThat(response.getHeader("Location")).startsWith("http://localhost:3000/account-status#code=ACCOUNT_BANNED&reason=");
        assertThat(refreshCookie(response)).isNull();
        assertUntouched(old);
        assertThat(count("SELECT count(*) FROM user_suspensions WHERE user_id = ? AND lifted_at IS NULL", old.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("data created after verification is never reset: a verified account keeps everything")
    void verifiedAccountIsNotReset() throws Exception {
        String email = AuthTestSupport.uniqueEmail();
        long id = support.userWithPassword(email, PASSWORD, Role.MEMBER, AccountStatus.ACTIVE, true);
        jdbc.update("INSERT INTO meal_plans (user_id, week_start) VALUES (?, date '2030-01-07')", id);

        signInWithGoogle(new Person("google-" + UUID.randomUUID(), email, "Chủ thật"));

        assertThat(count("SELECT count(*) FROM meal_plans WHERE user_id = ?", id)).isEqualTo(1);
        assertThat(recorder.events).isEmpty();
    }

    /** Nothing of the old account changed: same row, same data, no event, no Google identity. */
    private void assertUntouched(Preloaded old) {
        assertThat(recorder.events).as("no AccountResetEvent").isEmpty();
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT id, bio, password_hash IS NOT NULL AS has_password, google_sub, email_verified_at IS NOT NULL AS verified FROM users WHERE email = ?",
                old.email());
        assertThat(row).containsEntry("id", old.id()).containsEntry("bio", "Bio của kẻ gài sẵn").containsEntry("has_password", true)
                .containsEntry("verified", false);
        assertThat(row.get("google_sub")).isNull();
        assertThat(count("SELECT count(*) FROM user_allergens WHERE user_id = ?", old.id())).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM meal_plans WHERE user_id = ?", old.id())).isEqualTo(1);
        assertThat(storage.load(old.avatarKey())).isPresent();
    }
}
