package com.wikicook.profile.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.profile.support.ProfileFixtures;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** T031 (FR-009, FR-012, FR-013): the lists the form shows come from the server, none is hard-coded in the client. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class DietaryOptionsIT extends AbstractPostgresIT {

    private static final String URL = "/api/v1/me/dietary-options";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Test
    @DisplayName("FR-009: the ten allergen groups, with their Vietnamese name")
    void allergens() throws Exception {
        long user = support.member();

        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.allergens[*].code", hasItems("GLUTEN", "PEANUT", "TREE_NUT", "MILK", "EGG", "FISH",
                        "CRUSTACEAN", "MOLLUSC", "SOY", "SESAME")))
                .andExpect(jsonPath("$.allergens[?(@.code == 'PEANUT')].nameVi", contains("Đậu phộng")))
                .andExpect(jsonPath("$.allergens[0].id").isNumber());
    }

    @Test
    @DisplayName("FR-012: exactly the four diet types of the database, no Keto")
    void dietTypes() throws Exception {
        long user = support.member();

        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, user))
                .andExpect(jsonPath("$.dietTypes", contains("NONE", "VEGETARIAN", "VEGAN", "PESCATARIAN")))
                .andExpect(jsonPath("$.dietTypes", not(hasItems("KETO"))));
    }

    @Test
    @DisplayName("FR-013: only active cuisines, in the order of position then name")
    void cuisines() throws Exception {
        long user = support.member();
        ProfileFixtures fixtures = new ProfileFixtures(jdbc);
        String tag = UUID.randomUUID().toString().substring(0, 8);
        fixtures.cuisine("B-active-" + tag, true, 900);
        fixtures.cuisine("A-active-" + tag, true, 900);
        fixtures.cuisine("Z-first-" + tag, true, 899);
        fixtures.cuisine("Off-" + tag, false, 900);

        mvc.perform(get(URL).header(HeaderCurrentUser.HEADER, user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisines[*].name", hasItems("Z-first-" + tag, "A-active-" + tag, "B-active-" + tag)))
                .andExpect(jsonPath("$.cuisines[*].name", not(hasItems("Off-" + tag))))
                .andExpect(jsonPath("$.cuisines[?(@.name =~ /.*-" + tag + "/)].name",
                        contains("Z-first-" + tag, "A-active-" + tag, "B-active-" + tag)));
    }

    @Test
    @DisplayName("FR-006: the lists are for a signed-in caller only")
    void needsASignIn() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
