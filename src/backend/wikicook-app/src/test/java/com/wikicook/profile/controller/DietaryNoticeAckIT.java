package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * T123: {@code GET /me/dietary-profile} lists the notices of the caller and {@code POST .../notices/ack} closes them
 * (FR-019). The route answers 204 as in the contract; it removes only the caller's rows and ignores any other id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class DietaryNoticeAckIT extends AbstractPostgresIT {

    private static final String PROFILE = "/api/v1/me/dietary-profile";
    private static final String ACK = PROFILE + "/notices/ack";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    long user;
    long other;

    @BeforeEach
    void setUp() {
        user = support.member();
        other = support.member();
    }

    private long notice(long owner, String kind, String subject, String replacement) {
        return jdbc.queryForObject("""
                INSERT INTO dietary_notices (user_id, kind, subject_name, replacement_name) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, owner, kind, subject, replacement);
    }

    private ResultActions ack(long who, String json) throws Exception {
        return mvc.perform(post(ACK).header(HeaderCurrentUser.HEADER, who).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions profile(long who) throws Exception {
        return mvc.perform(get(PROFILE).header(HeaderCurrentUser.HEADER, who));
    }

    private List<Long> stored(long owner) {
        return jdbc.queryForList("SELECT id FROM dietary_notices WHERE user_id = ? ORDER BY id", Long.class, owner);
    }

    @Test
    @DisplayName("FR-019: the profile lists the pending notices, newest first, with the names at the time of the change")
    void profileListsPendingNotices() throws Exception {
        long merged = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");
        notice(user, "ALLERGEN_REMOVED", "Hai san", null);
        long removed = notice(user, "INGREDIENT_REMOVED", "Nam", null);

        profile(user).andExpect(status().isOk())
                .andExpect(jsonPath("$.notices", hasSize(3)))
                .andExpect(jsonPath("$.notices[0].id").value(removed))
                .andExpect(jsonPath("$.notices[2].id").value(merged))
                .andExpect(jsonPath("$.notices[2].kind").value("INGREDIENT_MERGED"))
                .andExpect(jsonPath("$.notices[2].subjectName").value("Ngo"))
                .andExpect(jsonPath("$.notices[2].replacementName").value("Ngo ri"));
    }

    @Test
    @DisplayName("FR-019: ack answers 204 and closes only the notices named; the rest stay")
    void ackClosesTheNamedNotices() throws Exception {
        long first = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");
        long second = notice(user, "INGREDIENT_REMOVED", "Nam", null);
        long third = notice(user, "ALLERGEN_REMOVED", "Hai san", null);

        ack(user, "{\"ids\":[" + first + "," + second + "]}").andExpect(status().isNoContent());

        assertThat(stored(user)).containsExactly(third);
        profile(user).andExpect(status().isOk()).andExpect(jsonPath("$.notices", hasSize(1)))
                .andExpect(jsonPath("$.notices[0].id").value(third));
    }

    @Test
    @DisplayName("FR-019: an id that belongs to someone else is ignored, not an error, and that notice stays")
    void ackIgnoresIdsOfOtherPeople() throws Exception {
        long mine = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");
        long theirs = notice(other, "INGREDIENT_REMOVED", "Nam", null);

        ack(user, "{\"ids\":[" + mine + "," + theirs + "]}").andExpect(status().isNoContent());

        assertThat(stored(user)).isEmpty();
        assertThat(stored(other)).containsExactly(theirs);
        profile(other).andExpect(status().isOk()).andExpect(jsonPath("$.notices[*].id", contains((int) theirs)));
    }

    @Test
    @DisplayName("FR-019: an unknown id, and a second ack of the same notice, are 204 and change nothing")
    void ackIsIdempotent() throws Exception {
        long mine = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");
        long kept = notice(user, "ALLERGEN_REMOVED", "Hai san", null);

        ack(user, "{\"ids\":[" + mine + ",999999999]}").andExpect(status().isNoContent());
        ack(user, "{\"ids\":[" + mine + "]}").andExpect(status().isNoContent());

        assertThat(stored(user)).containsExactly(kept);
    }

    @Test
    @DisplayName("FR-019: closing a notice does not touch the profile or its dietaryVersion (no write of the profile)")
    void ackDoesNotChangeTheProfile() throws Exception {
        long mine = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");
        int before = jdbc.queryForObject("SELECT dietary_version FROM users WHERE id = ?", Integer.class, user);

        ack(user, "{\"ids\":[" + mine + "]}").andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT dietary_version FROM users WHERE id = ?", Integer.class, user)).isEqualTo(before);
    }

    @Test
    @DisplayName("Edge: Invalid data: no ids, an empty list or a null id is 400 VALIDATION_ERROR on ids, nothing closed")
    void missingOrEmptyIdsAreRefused() throws Exception {
        long mine = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");

        for (String body : List.of("{}", "{\"ids\":null}", "{\"ids\":[]}")) {
            ack(user, body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[0].field").value("ids"))
                    .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
        }
        ack(user, "{\"ids\":[" + mine + ",null]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(stored(user)).containsExactly(mine);
    }

    @Test
    @DisplayName("Edge: Invalid data: more than 100 ids is 400 TOO_MANY_IDS; a body that is not JSON of that shape is 400")
    void tooManyIdsAndMalformedBodiesAreRefused() throws Exception {
        String tooMany = LongStream.rangeClosed(1, 101).mapToObj(Long::toString).collect(Collectors.joining(","));
        ack(user, "{\"ids\":[" + tooMany + "]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("ids"))
                .andExpect(jsonPath("$.errors[0].code").value("TOO_MANY_IDS"));

        ack(user, "{\"ids\":\"9\"}").andExpect(status().isBadRequest());
        ack(user, "{\"ids\":[\"a\"]}").andExpect(status().isBadRequest());
        ack(user, "not json").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FR-006: without a sign-in the route is 401 UNAUTHENTICATED and nothing is closed")
    void withoutSignInIs401() throws Exception {
        long mine = notice(user, "INGREDIENT_MERGED", "Ngo", "Ngo ri");

        mvc.perform(post(ACK).contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + mine + "]}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertThat(stored(user)).containsExactly(mine);
    }
}
