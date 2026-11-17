package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.AuthTestSupport.SignedIn;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * T112, SC-004, US2-7, US2-8: a person's email and dietary data reach nobody else. With the real security (JWT, not the
 * test header) it walks every GET route of the application as a guest, as a second Member and as an Administrator, and
 * scans every body for what was seeded for the victim: the email, a private ingredient name, a private cuisine name,
 * and the household size and time limit. The private names belong to rows that no catalogue lists (a proposed
 * ingredient, an inactive cuisine), so only a leak of the profile itself can show them. The Administrator may read the
 * email on {@code GET /admin/users/{id}} and nowhere else; nobody but the victim reads any dietary value.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestSupport.class)
class DataBoundaryIT extends AbstractPostgresIT {

    private static final String ADMIN_ACCOUNT = "/api/v1/admin/users/";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    SignedIn victim;
    SignedIn member;
    SignedIn administrator;
    String email;
    String secretIngredient;
    String secretCuisine;

    @BeforeEach
    void setUp() {
        ProfileFixtures fixtures = new ProfileFixtures(jdbc);
        victim = support.signedInMember();
        member = support.signedInMember();
        administrator = support.signedIn(Role.ADMINISTRATOR, AccountStatus.ACTIVE, true);
        email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, victim.userId());
        secretIngredient = "Nguyên liệu riêng tư " + UUID.randomUUID();
        secretCuisine = "Ẩm thực riêng tư " + UUID.randomUUID();
        long ingredient = fixtures.proposedIngredient(secretIngredient);
        long cuisine = fixtures.cuisine(secretCuisine, false, 0);
        jdbc.update("UPDATE users SET diet_type = 'VEGAN', household_size = 17, max_cook_time_minutes = 137, bio = 'Bio công khai' WHERE id = ?",
                victim.userId());
        jdbc.update("INSERT INTO user_allergens (user_id, allergen_id) VALUES (?, ?)", victim.userId(), fixtures.allergen("PEANUT"));
        jdbc.update("INSERT INTO user_excluded_ingredients (user_id, ingredient_id) VALUES (?, ?)", victim.userId(), ingredient);
        jdbc.update("INSERT INTO user_preferred_cuisines (user_id, cuisine_id) VALUES (?, ?)", victim.userId(), cuisine);
    }

    private record Route(String method, String pattern) {
    }

    private record Answer(String url, int status, String body) {
    }

    /** Every GET route under /api, without wildcards. */
    private List<Route> getRoutes() {
        Set<Route> routes = new LinkedHashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            boolean get = info.getMethodsCondition().getMethods().isEmpty()
                    || info.getMethodsCondition().getMethods().stream().anyMatch(m -> m.name().equals("GET"));
            if (!get) {
                continue;
            }
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (pattern.startsWith("/api/") && !pattern.contains("*")) {
                    routes.add(new Route("GET", pattern));
                }
            }
        }
        return new ArrayList<>(routes);
    }

    /** Fills the path variables so the route can be called; the victim's id goes where an id belongs. */
    private String fill(String pattern) {
        String monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1).toString();
        java.util.regex.Matcher variables = java.util.regex.Pattern.compile("\\{([^}:]+)(:[^}]*)?}").matcher(pattern);
        StringBuilder url = new StringBuilder();
        while (variables.find()) {
            String name = variables.group(1).toLowerCase();
            String value = name.contains("week") ? monday
                    : name.contains("day") ? "MONDAY"
                    : name.contains("meal") ? "LUNCH"
                    : name.equals("file") ? "x.jpg"
                    : name.contains("id") ? String.valueOf(victim.userId())
                    : "1";
            variables.appendReplacement(url, value);
        }
        variables.appendTail(url);
        return url.toString();
    }

    private Answer call(String url, SignedIn who) throws Exception {
        MockHttpServletRequestBuilder request = get(url);
        if (who != null) {
            request.header("Authorization", who.bearer());
        }
        var response = mvc.perform(request).andReturn().getResponse();
        return new Answer(url, response.getStatus(), response.getContentAsString());
    }

    private List<Answer> walk(SignedIn who) throws Exception {
        List<Answer> answers = new ArrayList<>();
        for (Route route : getRoutes()) {
            answers.add(call(fill(route.pattern()), who));
        }
        return answers;
    }

    private List<String> leaks(List<Answer> answers, boolean emailAllowedOnAdminAccount) {
        List<String> leaks = new ArrayList<>();
        for (Answer answer : answers) {
            boolean adminAccount = emailAllowedOnAdminAccount && answer.url().equals(ADMIN_ACCOUNT + victim.userId());
            if (!adminAccount && answer.body().contains(email)) {
                leaks.add("email in " + answer.url());
            }
            for (String secret : List.of(secretIngredient, secretCuisine, "\"householdSize\":17", "\"maxCookTimeMinutes\":137")) {
                if (answer.body().contains(secret)) {
                    leaks.add(secret + " in " + answer.url());
                }
            }
        }
        return leaks;
    }

    @Test
    @DisplayName("SC-004: the walk is not vacuous - it sees many routes, and the victim's own call does show the seeded data")
    void theSeededDataIsVisibleToItsOwner() throws Exception {
        assertThat(getRoutes().size()).isGreaterThan(15);

        Answer own = call("/api/v1/me/dietary-profile", victim);
        Answer ownProfile = call("/api/v1/me/profile", victim);

        assertThat(own.status()).isEqualTo(200);
        assertThat(own.body()).contains(secretIngredient).contains("\"householdSize\":17").contains("\"maxCookTimeMinutes\":137");
        assertThat(ownProfile.body()).contains(email);
    }

    @Test
    @DisplayName("SC-004, US2-7: a guest receives no email and no dietary value from any GET route")
    void guestSeesNothingPrivate() throws Exception {
        List<Answer> answers = walk(null);

        assertThat(leaks(answers, false)).isEmpty();
        assertThat(answers).filteredOn(a -> a.url().startsWith("/api/v1/me/")).allSatisfy(a -> assertThat(a.status()).isEqualTo(401));
    }

    @Test
    @DisplayName("SC-004, US2-7: a second Member receives no email and no dietary value of the victim from any GET route")
    void anotherMemberSeesNothingPrivate() throws Exception {
        List<Answer> answers = walk(member);

        assertThat(leaks(answers, false)).isEmpty();
    }

    @Test
    @DisplayName("SC-004, US2-8: an Administrator receives no dietary value of the victim from any GET route")
    void administratorSeesNoDietaryData() throws Exception {
        List<Answer> answers = walk(administrator);

        assertThat(leaks(answers, true)).isEmpty();
    }

    @Test
    @DisplayName("US2-8: an Administrator reads the victim's email and account on GET /admin/users/{id}, and only there")
    void administratorReadsTheAccount() throws Exception {
        Answer account = call(ADMIN_ACCOUNT + victim.userId(), administrator);

        assertThat(account.status()).isEqualTo(200);
        assertThat(account.body()).contains(email);
        assertThat(call(ADMIN_ACCOUNT + victim.userId(), member).status()).as("a Member is not an Administrator").isEqualTo(403);
        assertThat(call(ADMIN_ACCOUNT + victim.userId(), null).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("US2-8: /me/dietary-profile of an Administrator is the Administrator's own, not the victim's")
    void administratorGetsOnlyTheirOwnDietaryProfile() throws Exception {
        Answer answer = call("/api/v1/me/dietary-profile", administrator);

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).contains("\"empty\":true").doesNotContain(secretIngredient);
    }

    @Test
    @DisplayName("US2-6: a guest reads the public page of the victim with the real security, and it has no private field")
    void guestReadsThePublicPage() throws Exception {
        Answer page = call("/api/v1/profiles/" + victim.userId(), null);
        Answer recipes = call("/api/v1/profiles/" + victim.userId() + "/recipes", null);

        assertThat(page.status()).isEqualTo(200);
        assertThat(page.body()).contains("Bio công khai").doesNotContain(email).doesNotContain("householdSize");
        assertThat(recipes.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("US2-6: the media route of avatars is public, a missing file is 404 and not 401")
    void avatarRouteIsPublic() throws Exception {
        mvc.perform(get("/api/v1/media/avatars/00000000-0000-0000-0000-000000000000.jpg")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-018, US2-8: no write route of the profile module takes another person - every one is under /me")
    void noWriteRouteNamesAnotherPerson() {
        List<String> offenders = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getName().startsWith("com.wikicook.profile.controller")
                    || entry.getKey().getPathPatternsCondition() == null) {
                continue;
            }
            boolean write = entry.getKey().getMethodsCondition().getMethods().stream().anyMatch(m -> !m.name().equals("GET"));
            for (String pattern : entry.getKey().getPathPatternsCondition().getPatternValues()) {
                if (write && !pattern.startsWith("/api/v1/me/")) {
                    offenders.add(pattern);
                }
            }
        }
        assertThat(offenders).isEmpty();
    }
}
