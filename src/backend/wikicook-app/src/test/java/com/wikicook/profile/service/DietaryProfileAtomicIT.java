package com.wikicook.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.profile.repository.DietaryLinkRepository;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.profile.support.ProfileFixtures.Stored;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.sql.Timestamp;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** T029 (FR-017, NFR-007): the save is all or nothing, and rows that did not change keep their {@code created_at}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class DietaryProfileAtomicIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @MockitoSpyBean
    DietaryLinkRepository links;

    ProfileFixtures fixtures;
    long user;

    @BeforeEach
    void setUp() {
        fixtures = new ProfileFixtures(jdbc);
        user = support.member();
    }

    private org.springframework.test.web.servlet.ResultActions save(String json) throws Exception {
        return mvc.perform(put("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("FR-017: a failure on the third list rolls back the users columns and the first two lists")
    void failureOnTheThirdTableRollsEverythingBack() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        long ngoRi = fixtures.ingredient("Ngò rí");
        long vn = fixtures.cuisine("Việt Nam");
        save(ProfileFixtures.body(Set.of(peanut), Set.of(ngoRi), "VEGETARIAN", Set.of(vn), 4, 45, 0)).andExpect(status().isOk());
        Stored before = fixtures.stored(user);

        doThrow(new DataIntegrityViolationException("foreign key violation on the cuisines list"))
                .when(links).replaceCuisines(anyLong(), any());

        save(ProfileFixtures.body(Set.of(milk), Set.of(), "VEGAN", Set.of(fixtures.cuisine("Hàn Quốc")), 9, 90, 1))
                .andExpect(status().isBadRequest());

        assertThat(fixtures.stored(user)).as("nothing of the failed save is visible").isEqualTo(before);
    }

    @Test
    @DisplayName("FR-017: an unexpected failure (500) also leaves the stored profile as it was")
    void unexpectedFailureRollsBack() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "NONE", Set.of(), 2, null, 0)).andExpect(status().isOk());
        Stored before = fixtures.stored(user);

        doThrow(new IllegalStateException("disk full")).when(links).replaceExcludedIngredients(anyLong(), any());

        save(ProfileFixtures.body(Set.of(), Set.of(fixtures.ingredient("Ngò rí")), "VEGAN", Set.of(), 7, null, 1))
                .andExpect(status().isInternalServerError());

        assertThat(fixtures.stored(user)).isEqualTo(before);
    }

    @Test
    @DisplayName("R5 step 5: a row that does not change keeps its created_at, the new one is added")
    void unchangedRowsKeepTheirCreatedAt() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        save(ProfileFixtures.body(Set.of(peanut), Set.of(), "NONE", Set.of(), 2, null, 0)).andExpect(status().isOk());
        jdbc.update("UPDATE user_allergens SET created_at = now() - interval '3 days' WHERE user_id = ?", user);
        Timestamp original = jdbc.queryForObject("SELECT created_at FROM user_allergens WHERE user_id = ? AND allergen_id = ?",
                Timestamp.class, user, peanut);

        save(ProfileFixtures.body(Set.of(peanut, milk), Set.of(), "NONE", Set.of(), 3, null, 1)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT created_at FROM user_allergens WHERE user_id = ? AND allergen_id = ?",
                Timestamp.class, user, peanut)).isEqualTo(original);
        assertThat(fixtures.stored(user).allergenIds()).containsExactlyInAnyOrder(peanut, milk);
    }

    @Test
    @DisplayName("R5 step 5: a row that was removed from the form is deleted")
    void removedRowsAreDeleted() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        save(ProfileFixtures.body(Set.of(peanut, milk), Set.of(), "NONE", Set.of(), null, null, 0)).andExpect(status().isOk());

        save(ProfileFixtures.body(Set.of(milk), Set.of(), "NONE", Set.of(), null, null, 1)).andExpect(status().isOk());

        assertThat(fixtures.stored(user).allergenIds()).containsExactly(milk);
    }
}
