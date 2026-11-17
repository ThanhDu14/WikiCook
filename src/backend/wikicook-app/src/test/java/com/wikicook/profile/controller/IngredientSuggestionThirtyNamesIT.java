package com.wikicook.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T033, SC-007: 30 Vietnamese ingredient names, typed without diacritics (also in capitals); the right name is among the
 * first 5 suggestions in every case. Several names share their first word ("Hành …", "Đậu …", "Thịt …"), so the rank
 * matters, and four of them contain "đ".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class IngredientSuggestionThirtyNamesIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    long user;
    List<String> names;

    @BeforeEach
    void setUp() throws IOException {
        user = support.member();
        names = new ArrayList<>(new ClassPathResource("ingredients/sc007-names.csv").getContentAsString(StandardCharsets.UTF_8)
                .lines().skip(1).map(String::strip).filter(line -> !line.isEmpty()).toList());
        ProfileFixtures fixtures = new ProfileFixtures(jdbc);
        names.forEach(fixtures::ingredient);
    }

    /** What a person without a Vietnamese keyboard types: no marks, and "đ" as "d". */
    private static String withoutDiacritics(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{Mn}+", "").replace('đ', 'd').replace('Đ', 'D');
    }

    private List<String> firstFive(String query) throws Exception {
        String json = mvc.perform(get("/api/v1/ingredients/suggestions").param("q", query)
                        .header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        List<String> all = JsonPath.read(json, "$[*].name");
        return all.stream().limit(5).toList();
    }

    @Test
    @DisplayName("SC-007: the list has 30 names and some of them contain đ")
    void theListIsTheAgreedOne() {
        assertThat(names).hasSize(30).doesNotHaveDuplicates();
        assertThat(names.stream().filter(name -> name.toLowerCase(Locale.ROOT).contains("đ")).count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("SC-007: each name typed without diacritics is among the first 5 suggestions (100%)")
    void typedWithoutDiacritics() throws Exception {
        List<String> misses = new ArrayList<>();
        for (String name : names) {
            String typed = withoutDiacritics(name).toLowerCase(Locale.ROOT);
            if (!firstFive(typed).contains(name)) {
                misses.add(name + " (typed '" + typed + "')");
            }
        }
        assertThat(misses).as("names missing from the first five").isEmpty();
    }

    @Test
    @DisplayName("SC-007: each name typed in capitals without diacritics is among the first 5 suggestions (100%)")
    void typedInCapitals() throws Exception {
        List<String> misses = new ArrayList<>();
        for (String name : names) {
            String typed = withoutDiacritics(name).toUpperCase(Locale.ROOT);
            if (!firstFive(typed).contains(name)) {
                misses.add(name + " (typed '" + typed + "')");
            }
        }
        assertThat(misses).isEmpty();
    }

    @Test
    @DisplayName("SC-007: each name typed with its own diacritics is among the first 5 suggestions (100%)")
    void typedWithDiacritics() throws Exception {
        List<String> misses = new ArrayList<>();
        for (String name : names) {
            if (!firstFive(name).contains(name)) {
                misses.add(name);
            }
        }
        assertThat(misses).isEmpty();
    }

    @Test
    @DisplayName("SC-007: the name of a shared first word comes first when it is typed in full ('hanh la')")
    void sharedFirstWord() throws Exception {
        assertThat(firstFive("hanh la").getFirst()).isEqualTo("Hành lá");
        assertThat(firstFive("dau phong").getFirst()).isEqualTo("Đậu phộng");
        assertThat(firstFive("duong cat").getFirst()).isEqualTo("Đường cát");
    }
}
