package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import com.wikicook.support.StatementCounting;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** T032 (US1-3, research R6): {@code GET /ingredients/suggestions}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class, StatementCounting.class})
class IngredientSuggestionIT extends AbstractPostgresIT {

    private static final String URL = "/api/v1/ingredients/suggestions";

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

    private List<String> names(String q) throws Exception {
        String json = mvc.perform(get(URL).param("q", q).header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].name");
    }

    @Test
    @DisplayName("US1-3: 'ngo', 'Ngò' and 'NGO' give the same list and include 'Ngò rí'")
    void sameResultWithOrWithoutDiacriticsAndCase() throws Exception {
        fixtures.ingredient("Ngò rí");

        List<String> typed = names("ngo");

        assertThat(typed).contains("Ngò rí");
        assertThat(names("Ngò")).isEqualTo(typed);
        assertThat(names("NGO")).isEqualTo(typed);
        assertThat(names("  ngo  ")).isEqualTo(typed);
    }

    @Test
    @DisplayName("R6: exact match first, then start of the name, then start of a word, then anywhere")
    void ranking() throws Exception {
        String stem = "zingiber" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        fixtures.ingredient("Wild a" + stem); // contains the text in the middle of a word
        fixtures.ingredient("Rau " + stem + " xanh"); // starts a later word
        fixtures.ingredient(stem + " officinale"); // starts the name
        fixtures.ingredient(stem); // is the text

        assertThat(names(stem)).containsExactly(stem, stem + " officinale", "Rau " + stem + " xanh", "Wild a" + stem);
    }

    @Test
    @DisplayName("US1-3: only APPROVED ingredients are offered")
    void onlyApprovedIngredients() throws Exception {
        String stem = "quyetdinh" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        fixtures.ingredient(stem + " duoc duyet");
        fixtures.proposedIngredient(stem + " chua duyet");

        assertThat(names(stem)).containsExactly(stem + " duoc duyet");
    }

    @Test
    @DisplayName("R6: at most 10 suggestions")
    void atMostTen() throws Exception {
        String stem = "gioihan" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        for (int i = 0; i < 12; i++) {
            fixtures.ingredient(stem + " " + (char) ('a' + i));
        }

        mvc.perform(get(URL).param("q", stem).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    @DisplayName("US1-3: fewer than two characters is an empty list and no query reaches the database")
    void shortQueryDoesNotTouchTheDatabase() throws Exception {
        StatementCounting.reset();

        mvc.perform(get(URL).param("q", "n").header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(URL).param("q", " a ").header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(URL).param("q", "").header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertThat(StatementCounting.statements()).noneMatch(sql -> sql.toLowerCase().contains("ingredients"));
    }

    @Test
    @DisplayName("US1-3: two characters are enough (names such as 'gà' and 'ổi')")
    void twoCharactersAreEnough() throws Exception {
        fixtures.ingredient("Gà ta");

        assertThat(names("gà")).contains("Gà ta");
    }

    @Test
    @DisplayName("Constitution IV: %, _ and \\ in q are text, not wildcards")
    void wildcardsAreEscaped() throws Exception {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        fixtures.ingredient("Ot 100% cay " + tag);
        fixtures.ingredient("Zab" + tag);
        fixtures.ingredient("Duong\\vi " + tag);

        assertThat(names("100% cay " + tag)).containsExactly("Ot 100% cay " + tag);
        assertThat(names("z_b" + tag)).isEmpty(); // '_' would match 'a' as a wildcard
        assertThat(names("%%")).isEmpty(); // '%' would match everything
        assertThat(names("duong\\vi")).contains("Duong\\vi " + tag);
        assertThat(names("a\\")).doesNotContain("Zab" + tag);
    }

    @Test
    @DisplayName("R6: regular expression characters in q are text too (the word-start rank uses a regex)")
    void regexCharactersAreEscaped() throws Exception {
        assertThat(names("(((")).isEmpty();
        assertThat(names("[a-z]+")).isEmpty();
        assertThat(names(".*")).isEmpty();
        assertThat(names("a|b")).isEmpty();
    }

    @Test
    @DisplayName("Only name and id are returned, never kind or status")
    void onlyIdAndName() throws Exception {
        fixtures.ingredient("Ngò rí");

        mvc.perform(get(URL).param("q", "ngo rí").header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].kind").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    @DisplayName("openapi: q longer than 50 characters is a 400, a missing q too")
    void queryLengthIsLimited() throws Exception {
        mvc.perform(get(URL).param("q", "a".repeat(51)).header(HeaderCurrentUser.HEADER, user))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get(URL).param("q", "a".repeat(50)).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk());
        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FR-006: a sign-in is required")
    void needsASignIn() throws Exception {
        mvc.perform(get(URL).param("q", "ngo")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
