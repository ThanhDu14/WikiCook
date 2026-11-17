package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.profile.support.ProfileFixtures.Stored;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** T028: {@code GET}, {@code PUT /me/dietary-profile} end to end (US1-1, 2, 4, 5, 6, 7; Edge: Invalid data). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class DietaryProfileControllerIT extends AbstractPostgresIT {

    private static final String URL = "/api/v1/me/dietary-profile";

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

    private ResultActions load(long who) throws Exception {
        return mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, who));
    }

    private ResultActions save(String json) throws Exception {
        return mvc.perform(put(URL).header(HeaderCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(Set<Long> allergens, Set<Long> excluded, String diet, Set<Long> cuisines, Object household,
            Object cook, int version) {
        return ProfileFixtures.body(allergens, excluded, diet, cuisines, household, cook, version);
    }

    @Test
    @DisplayName("US1-1: a new account has an empty profile, answered with 200 and empty = true")
    void newAccountHasAnEmptyProfile() throws Exception {
        load(user).andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.dietType").value("NONE"))
                .andExpect(jsonPath("$.householdSize").value(nullValue()))
                .andExpect(jsonPath("$.maxCookTimeMinutes").value(nullValue()))
                .andExpect(jsonPath("$.allergens", hasSize(0)))
                .andExpect(jsonPath("$.excludedIngredients", hasSize(0)))
                .andExpect(jsonPath("$.preferredCuisines", hasSize(0)))
                .andExpect(jsonPath("$.notices", hasSize(0)));
    }

    @Test
    @DisplayName("US1-2, US1-4, US1-6: every value saved comes back, from the API and from the tables")
    void roundTrip() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        long ngoRi = fixtures.ingredient("Ngò rí");
        long vn = fixtures.cuisine("Việt Nam");
        long jp = fixtures.cuisine("Nhật Bản");

        save(body(Set.of(peanut), Set.of(ngoRi), "VEGETARIAN", Set.of(vn, jp), 4, 45, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.dietType").value("VEGETARIAN"))
                .andExpect(jsonPath("$.householdSize").value(4))
                .andExpect(jsonPath("$.maxCookTimeMinutes").value(45))
                .andExpect(jsonPath("$.allergens[0].code").value("PEANUT"))
                .andExpect(jsonPath("$.excludedIngredients[0].name").value("Ngò rí"))
                .andExpect(jsonPath("$.preferredCuisines[*].name", containsInAnyOrder("Việt Nam", "Nhật Bản")));

        load(user).andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.allergens[0].id").value(peanut))
                .andExpect(jsonPath("$.allergens[0].nameVi").value("Đậu phộng"))
                .andExpect(jsonPath("$.excludedIngredients[0].id").value(ngoRi))
                .andExpect(jsonPath("$.dietType").value("VEGETARIAN"))
                .andExpect(jsonPath("$.preferredCuisines", hasSize(2)))
                .andExpect(jsonPath("$.householdSize").value(4))
                .andExpect(jsonPath("$.maxCookTimeMinutes").value(45));

        assertThat(fixtures.stored(user)).isEqualTo(new Stored("VEGETARIAN", 4, 45, 1, Set.of(peanut), Set.of(ngoRi),
                Set.of(vn, jp)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NONE", "VEGETARIAN", "VEGAN", "PESCATARIAN"})
    @DisplayName("US1-4: each of the four diet types is accepted")
    void everyDietTypeIsAccepted(String diet) throws Exception {
        save(body(Set.of(), Set.of(), diet, Set.of(), null, null, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.dietType").value(diet));
    }

    @Test
    @DisplayName("US1-4: Keto and any other diet type is 400 on dietType, nothing is saved")
    void unknownDietTypeIsRefused() throws Exception {
        save(body(Set.of(), Set.of(), "KETO", Set.of(), null, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("dietType"));
        assertThat(fixtures.stored(user).dietaryVersion()).isZero();
    }

    @Test
    @DisplayName("US1-2: an allergen outside the fixed list is UNKNOWN_ALLERGEN; the stored profile is unchanged")
    void unknownAllergenIsRefused() throws Exception {
        long vn = fixtures.cuisine("Việt Nam");
        save(body(Set.of(), Set.of(), "VEGAN", Set.of(vn), 3, null, 0)).andExpect(status().isOk());
        Stored before = fixtures.stored(user);

        save(body(Set.of(999_999L), Set.of(), "NONE", Set.of(), null, null, 1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("allergenIds"))
                .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_ALLERGEN"));

        assertThat(fixtures.stored(user)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "2.5", "-1", "\"abc\""})
    @DisplayName("US1-5: householdSize 0, 21, 2.5 and non-numbers are refused beside the field")
    void badHouseholdSizeIsRefused(String value) throws Exception {
        save(body(Set.of(), Set.of(), "NONE", Set.of(), value, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("householdSize"));
        assertThat(fixtures.stored(user).householdSize()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "9", "241", "2.5", "-10"})
    @DisplayName("US1-5: maxCookTimeMinutes 5, 241, 2.5 are refused beside the field")
    void badMaxCookTimeIsRefused(String value) throws Exception {
        save(body(Set.of(), Set.of(), "NONE", Set.of(), null, value, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("maxCookTimeMinutes"));
        assertThat(fixtures.stored(user).maxCookTimeMinutes()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "20", "10", "null"})
    @DisplayName("US1-5: householdSize 1, 20 and empty are accepted")
    void goodHouseholdSizeIsAccepted(String value) throws Exception {
        save(body(Set.of(), Set.of(), "NONE", Set.of(), value, null, 0)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "240", "45", "null"})
    @DisplayName("US1-5: maxCookTimeMinutes 10, 240 and empty are accepted")
    void goodMaxCookTimeIsAccepted(String value) throws Exception {
        save(body(Set.of(), Set.of(), "NONE", Set.of(), null, value, 0)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("US1-7: clearing every field is saved and the profile is empty again")
    void clearingEverything() throws Exception {
        long peanut = fixtures.allergen("PEANUT");
        save(body(Set.of(peanut), Set.of(fixtures.ingredient("Ngò rí")), "VEGAN", Set.of(fixtures.cuisine("Việt Nam")), 4, 45, 0))
                .andExpect(status().isOk());

        save(body(Set.of(), Set.of(), "NONE", Set.of(), null, null, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true));

        load(user).andExpect(jsonPath("$.empty").value(true)).andExpect(jsonPath("$.allergens", hasSize(0)));
        assertThat(fixtures.stored(user)).isEqualTo(new Stored("NONE", null, null, 2, Set.of(), Set.of(), Set.of()));
    }

    @Test
    @DisplayName("Edge: 51st ingredient - 51 excluded ingredients is EXCLUDED_LIMIT_EXCEEDED, 50 is accepted")
    void excludedLimit() throws Exception {
        Set<Long> fiftyOne = LongStream.rangeClosed(1, 51).boxed().collect(Collectors.toSet());
        save(body(Set.of(), fiftyOne, "NONE", Set.of(), null, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("excludedIngredientIds"))
                .andExpect(jsonPath("$.errors[0].code").value("EXCLUDED_LIMIT_EXCEEDED"));

        Set<Long> fifty = LongStream.range(0, 50).mapToObj(i -> fixtures.ingredient("Nguyên liệu giới hạn " + i))
                .collect(Collectors.toSet());
        save(body(Set.of(), fifty, "NONE", Set.of(), null, null, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.excludedIngredients", hasSize(50)));
    }

    @Test
    @DisplayName("Edge: Invalid data - a new exclusion must be an APPROVED ingredient, a new cuisine an active one")
    void catalogueRulesForNewIds() throws Exception {
        long proposed = fixtures.proposedIngredient("Nguyên liệu chưa duyệt " + System.nanoTime());
        long inactive = fixtures.cuisine("Ẩm thực đã tắt", false, 0);

        save(body(Set.of(), Set.of(proposed), "NONE", Set.of(), null, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("excludedIngredientIds"))
                .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_INGREDIENT"));
        save(body(Set.of(), Set.of(), "NONE", Set.of(inactive), null, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("preferredCuisineIds"))
                .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_CUISINE"));
        save(body(Set.of(), Set.of(123_456_789L), "NONE", Set.of(), null, null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_INGREDIENT"));
    }

    @Test
    @DisplayName("Edge: Invalid data - a saved exclusion stays valid after the catalogue withdraws it")
    void savedIdsSurviveCatalogueChanges() throws Exception {
        long ingredient = fixtures.ingredient("Nguyên liệu bị rút " + System.nanoTime());
        long cuisine = fixtures.cuisine("Ẩm thực sẽ tắt");
        save(body(Set.of(), Set.of(ingredient), "NONE", Set.of(cuisine), null, null, 0)).andExpect(status().isOk());
        jdbc.update("UPDATE ingredients SET status = 'REJECTED' WHERE id = ?", ingredient);
        jdbc.update("UPDATE cuisines SET is_active = false WHERE id = ?", cuisine);

        save(body(Set.of(), Set.of(ingredient), "NONE", Set.of(cuisine), 3, null, 1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.excludedIngredients[0].id").value(ingredient))
                .andExpect(jsonPath("$.householdSize").value(3));
    }

    @Test
    @DisplayName("Edge: Invalid data - a field that is missing is refused, so it can not clear an allergy list silently")
    void missingFieldsAreRefused() throws Exception {
        save("{\"dietType\": \"NONE\", \"version\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        save("{\"allergenIds\": [], \"excludedIngredientIds\": [], \"dietType\": \"NONE\", \"preferredCuisineIds\": []}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("version"));
        save("not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Constitution IV: a refusal never shows a stack trace, a class name or the SQL")
    void refusalsLeakNothing() throws Exception {
        String response = save(body(Set.of(999_999L), Set.of(), "NONE", Set.of(), 0, null, 0)).andReturn().getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("Exception").doesNotContain("com.wikicook").doesNotContain("SELECT");
    }

    @Test
    @DisplayName("US1-6: the profile is the caller's own - a second user sees an empty one")
    void theProfileBelongsToTheCaller() throws Exception {
        save(body(Set.of(fixtures.allergen("MILK")), Set.of(), "VEGAN", Set.of(), 6, null, 0)).andExpect(status().isOk());
        long other = support.member();

        load(other).andExpect(status().isOk()).andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.allergens", hasSize(0)));
    }

    @Test
    @DisplayName("FR-006: without a sign-in both routes are 401 UNAUTHENTICATED")
    void withoutSignInIs401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Set.of(), Set.of(), "NONE", Set.of(), null, null, 0)))
                .andExpect(status().isUnauthorized());
    }
}
