package com.wikicook.profile.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import com.wikicook.common.diet.DietaryProfileUnavailableException;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.profile.repository.DietaryLinkRepository;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.shoppinglist.port.DietaryProfilePort.UserDietaryRestrictions;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * T034 (US1-8, FR-023, FR-026, FR-026a): the two ports 030, 031, 040 and 020 read the profile through, with the dev
 * stubs switched on so the test also proves the real beans win over {@code DevStubConfig} and {@code DefaultPortsConfig}.
 */
@SpringBootTest(properties = "wikicook.dev-stubs.enabled=true")
@Import(AuthTestSupport.class)
class DietaryPortsIT extends AbstractPostgresIT {

    @Autowired
    com.wikicook.mealplan.port.DietaryProfilePort planners;

    @Autowired
    com.wikicook.shoppinglist.port.DietaryProfilePort shopping;

    @Autowired
    ApplicationContext context;

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

    private void store(long userId, Set<Long> allergens, Set<Long> excluded, String diet, Set<Long> cuisines,
            Integer household, Integer cook) {
        jdbc.update("UPDATE users SET diet_type = ?, household_size = ?, max_cook_time_minutes = ? WHERE id = ?", diet,
                household, cook, userId);
        allergens.forEach(id -> jdbc.update("INSERT INTO user_allergens (user_id, allergen_id) VALUES (?, ?)", userId, id));
        excluded.forEach(id -> jdbc.update("INSERT INTO user_excluded_ingredients (user_id, ingredient_id) VALUES (?, ?)", userId, id));
        cuisines.forEach(id -> jdbc.update("INSERT INTO user_preferred_cuisines (user_id, cuisine_id) VALUES (?, ?)", userId, id));
    }

    @Test
    @DisplayName("US1-8: findByUserId returns the six fields of what the user saved")
    void plannerProfileHasTheSixFields() {
        long peanut = fixtures.allergen("PEANUT");
        long ngoRi = fixtures.ingredient("Ngò rí");
        long vn = fixtures.cuisine("Việt Nam");
        store(user, Set.of(peanut), Set.of(ngoRi), "PESCATARIAN", Set.of(vn), 4, 45);

        PlannerProfile profile = planners.findByUserId(user);

        assertThat(profile).isEqualTo(new PlannerProfile(4, DietType.PESCATARIAN, 45, Set.of(peanut), Set.of(ngoRi), Set.of(vn)));
    }

    @Test
    @DisplayName("FR-023: an empty household size stays null for the reader, who applies the default")
    void emptyHouseholdSizeIsNull() {
        store(user, Set.of(), Set.of(), "NONE", Set.of(), null, null);

        PlannerProfile profile = planners.findByUserId(user);

        assertThat(profile.householdSize()).isNull();
        assertThat(profile.maxCookTimeMinutes()).isNull();
        assertThat(profile).isEqualTo(PlannerProfile.empty());
    }

    @Test
    @DisplayName("FR-026: no users row is an empty profile for both ports, not an error")
    void unknownUserIsEmpty() {
        long missing = jdbc.queryForObject("SELECT COALESCE(max(id), 0) + 100000 FROM users", Long.class);

        assertThat(planners.findByUserId(missing)).isEqualTo(PlannerProfile.empty());
        assertThat(shopping.getRestrictions(missing)).isEqualTo(new UserDietaryRestrictions(Set.of(), Set.of()));
    }

    @Test
    @DisplayName("FR-010, FR-026: 040 gets INGREDIENT ids derived from ingredient_allergens, plus the exclusions")
    void shoppingGetsIngredientIds() {
        long peanut = fixtures.allergen("PEANUT");
        long milk = fixtures.allergen("MILK");
        long peanutButter = fixtures.ingredient("Bơ đậu phộng", "PLANT");
        long peanutOil = fixtures.ingredient("Dầu đậu phộng", "PLANT");
        long butter = fixtures.ingredient("Bơ sữa", "DAIRY");
        long salt = fixtures.ingredient("Muối", "OTHER");
        fixtures.tagIngredientWithAllergen(peanutButter, peanut);
        fixtures.tagIngredientWithAllergen(peanutOil, peanut);
        fixtures.tagIngredientWithAllergen(butter, milk); // the user is not allergic to milk
        long ngoRi = fixtures.ingredient("Ngò rí");
        store(user, Set.of(peanut), Set.of(ngoRi, salt), "NONE", Set.of(), null, null);

        UserDietaryRestrictions restrictions = shopping.getRestrictions(user);

        assertThat(restrictions.allergenIngredientIds()).containsExactlyInAnyOrder(peanutButter, peanutOil);
        assertThat(restrictions.allergenIngredientIds()).doesNotContain(peanut); // an ingredient id, not allergens.id
        assertThat(restrictions.excludedIngredientIds()).containsExactlyInAnyOrder(ngoRi, salt);
    }

    @Test
    @DisplayName("FR-026a: a database failure is DietaryProfileUnavailableException from both ports, never an empty profile")
    void readFailureIsNotAnEmptyProfile() {
        doThrow(new DataAccessResourceFailureException("connection reset")).when(links).findSnapshot(user);

        assertThatThrownBy(() -> planners.findByUserId(user)).isInstanceOf(DietaryProfileUnavailableException.class);
        assertThatThrownBy(() -> shopping.getRestrictions(user)).isInstanceOf(DietaryProfileUnavailableException.class);
    }

    @Test
    @DisplayName("R8: the real adapters are the beans in use; the dev stub and the empty default stepped aside")
    void realBeansWinOverTheStubs() {
        assertThat(context.getBeansOfType(com.wikicook.mealplan.port.DietaryProfilePort.class)).hasSize(1);
        assertThat(planners).isInstanceOf(MealPlanDietaryAdapter.class);
        assertThat(shopping).isInstanceOf(ShoppingDietaryAdapter.class);
        assertThat(context.getBeanNamesForType(com.wikicook.mealplan.port.DietaryProfilePort.class))
                .doesNotContain("stubDietaryProfile", "emptyDietaryProfile");
    }

    @Test
    @DisplayName("FR-024, FR-025: the ports read the tables at call time, so a later save is seen by the next call")
    void nextCallSeesTheNewValue() {
        store(user, Set.of(), Set.of(), "NONE", Set.of(), 2, null);
        assertThat(planners.findByUserId(user).householdSize()).isEqualTo(2);

        jdbc.update("UPDATE users SET household_size = 6 WHERE id = ?", user);

        assertThat(planners.findByUserId(user).householdSize()).isEqualTo(6);
    }
}
