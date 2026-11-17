package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.profile.port.ProfileAuditPort;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.profile.support.ProfileFixtures.Stored;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T030: two tabs of one profile and repeated clicks (research R5, R12, FR-041). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class, ProfileConflictIT.RecordingAudit.class})
class ProfileConflictIT extends AbstractPostgresIT {

    record Recorded(long userId, String action, Map<String, ?> details) {
    }

    /** Stands in for the audit adapter of 010 (T116) and remembers what the profile module recorded. */
    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingAudit {

        static final List<Recorded> RECORDS = new CopyOnWriteArrayList<>();

        @Bean
        ProfileAuditPort recordingAudit() {
            return (userId, action, details) -> RECORDS.add(new Recorded(userId, action, details));
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    ProfileFixtures fixtures;
    long user;

    @BeforeEach
    void setUp() {
        fixtures = new ProfileFixtures(jdbc);
        user = support.member();
    }

    private ResultActions save(String json) throws Exception {
        return mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static List<Recorded> auditOf(long userId) {
        return RecordingAudit.RECORDS.stream().filter(r -> r.userId() == userId).toList();
    }

    @Test
    @DisplayName("Edge: Two tabs - the tab with the old version is told, and saving over it wins for the whole form")
    void staleTabIsToldAndCanOverwrite() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        mvc.perform(get("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.version").value(0)); // both tabs loaded version 0

        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "VEGAN", Set.of(), 3, null, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1)); // tab A saves

        save(ProfileFixtures.body(Set.of(milk), Set.of(), "PESCATARIAN", Set.of(), 5, 60, 0)) // tab B, still on version 0
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_CHANGED"));
        assertThat(fixtures.stored(user)).isEqualTo(new Stored("VEGAN", 3, null, 1, Set.of(peanut), Set.of(), Set.of()));

        // B chooses "Lưu đè": reads the new version and sends the same form again
        mvc.perform(get("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.version").value(1));
        save(ProfileFixtures.body(Set.of(milk), Set.of(), "PESCATARIAN", Set.of(), 5, 60, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        assertThat(fixtures.stored(user)).isEqualTo(new Stored("PESCATARIAN", 5, 60, 2, Set.of(milk), Set.of(), Set.of()));
    }

    @Test
    @DisplayName("Edge: Repeated clicks - the same form twice is 200 twice, one version step, one audit row")
    void sameFormTwiceHasOneEffect() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        String form = ProfileFixtures.body(Set.of(peanut), Set.of(), "VEGETARIAN", Set.of(), 4, 45, 0);

        save(form).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        save(form).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)); // same stale version, same content

        assertThat(fixtures.stored(user).dietaryVersion()).isEqualTo(1);
        assertThat(auditOf(user)).hasSize(1);
        assertThat(auditOf(user).getFirst().action()).isEqualTo("DIETARY_PROFILE_UPDATED");
        assertThat(auditOf(user).getFirst().details()).isEmpty(); // FR-042: no dietary value, not even a field name
    }

    @Test
    @DisplayName("Edge: Repeated clicks - the same content with an old version is 200, not a false conflict")
    void sameContentWithAnOldVersionIsNotAConflict() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "NONE", Set.of(), 2, null, 0)).andExpect(status().isOk());
        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "NONE", Set.of(), 3, null, 1)).andExpect(status().isOk());

        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "NONE", Set.of(), 3, null, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        assertThat(fixtures.stored(user).dietaryVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Edge: Two tabs - a rejected save leaves no audit row either")
    void conflictIsNotAudited() throws Exception {
        save(ProfileFixtures.body(Set.of(), Set.of(), "VEGAN", Set.of(), null, null, 0)).andExpect(status().isOk());
        int before = auditOf(user).size();

        save(ProfileFixtures.body(Set.of(), Set.of(), "PESCATARIAN", Set.of(), null, null, 0)).andExpect(status().isConflict());

        assertThat(auditOf(user)).hasSize(before);
    }
}
