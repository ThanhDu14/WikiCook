package com.wikicook.search.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.search.support.SearchFixtures;
import com.wikicook.support.AbstractPostgresIT;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP shape of 020 contracts/openapi.yaml, end to end on the real database. */
@SpringBootTest(properties = "wikicook.dev-stubs.enabled=true")
@AutoConfigureMockMvc
class RecipeSearchControllerIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    DietaryProfilePort profiles;

    SearchFixtures fx;
    long peanutAllergen;
    long safe;
    long withPeanut;

    @BeforeEach
    void setUp() {
        SearchFixtures.clear(jdbc);
        fx = new SearchFixtures(jdbc);
        peanutAllergen = fx.allergen("PEANUT", "Đậu phộng");
        long peanut = fx.ingredient("Đậu phộng", "PLANT");
        fx.linkAllergen(peanut, peanutAllergen);
        safe = fx.recipe("Phở bò Hà Nội").rating(4.7, 88).create();
        withPeanut = fx.recipe("Phở trộn đậu phộng").ingredients(peanut).create();
        when(profiles.findByUserId(anyLong())).thenReturn(PlannerProfile.empty());
    }

    @Test
    void keywordSearchReturnsContractShape() throws Exception {
        mvc.perform(get("/api/v1/recipes/search").param("q", "pho bo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(safe))
                .andExpect(jsonPath("$.items[0].title").value("Phở bò Hà Nội"))
                .andExpect(jsonPath("$.items[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.items[0].ratingAvg").value(4.7))
                .andExpect(jsonPath("$.items[0].reviewCount").value(88))
                .andExpect(jsonPath("$.items[0].coverImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[0].conflicts").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.sort").value("RELEVANCE"))
                .andExpect(jsonPath("$.keyword").value("pho bo"))
                .andExpect(jsonPath("$.keywordTruncated").value(false))
                .andExpect(jsonPath("$.appliedFilters.time").value(nullValue()))
                .andExpect(jsonPath("$.appliedFilters.cuisines").isEmpty())
                .andExpect(jsonPath("$.appliedFilters.diet").value(nullValue()))
                .andExpect(jsonPath("$.dietary.profileAvailable").value(false));
    }

    @Test
    void specialCharactersNeverCauseAnError() throws Exception {
        mvc.perform(get("/api/v1/recipes/search").param("q", "%*<script>'\"&|!:()"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("%*<script>'\"&|!:()"));
    }

    @Test
    void invalidValuesAreIgnored() throws Exception {
        mvc.perform(get("/api/v1/recipes/search")
                        .param("cuisine", "khong-ton-tai").param("time", "LE45").param("page", "999")
                        .param("sort", "RANDOM").param("showHidden", "maybe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedFilters.cuisines").isEmpty())
                .andExpect(jsonPath("$.sort").value("NEWEST"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void repeatedFilterParamsCombine() throws Exception {
        long japanese = fx.recipe("Ramen Nhật").cuisine(fx.japan).create();

        mvc.perform(get("/api/v1/recipes/search")
                        .param("cuisine", fx.slug("viet-nam")).param("cuisine", fx.slug("nhat-ban")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.appliedFilters.cuisines[*].name").value(contains("Việt Nam", "Nhật Bản")));

        mvc.perform(get("/api/v1/recipes/search").param("cuisine", fx.slug("nhat-ban")))
                .andExpect(jsonPath("$.items[*].id").value(contains((int) japanese)));
    }

    @Test
    void guestSeesEverything() throws Exception {
        when(profiles.findByUserId(anyLong()))
                .thenReturn(new PlannerProfile(null, DietType.NONE, null, Set.of(peanutAllergen), Set.of(), Set.of()));

        mvc.perform(get("/api/v1/recipes/search").param("q", "pho"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.dietary.profileAvailable").value(false))
                .andExpect(jsonPath("$.dietary.hiddenCount").value(0));
    }

    @Test
    void signedInUserWithAllergyHasRecipesHidden() throws Exception {
        when(profiles.findByUserId(5L))
                .thenReturn(new PlannerProfile(null, DietType.NONE, null, Set.of(peanutAllergen), Set.of(), Set.of()));

        mvc.perform(get("/api/v1/recipes/search").param("q", "pho").header("X-Dev-User-Id", "5"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].id").value(not(hasItem((int) withPeanut))))
                .andExpect(jsonPath("$.dietary.applied").value(true))
                .andExpect(jsonPath("$.dietary.hiddenCount").value(1))
                .andExpect(jsonPath("$.dietary.showingHidden").value(false));
    }

    @Test
    void showHiddenReturnsConflicts() throws Exception {
        when(profiles.findByUserId(5L))
                .thenReturn(new PlannerProfile(null, DietType.NONE, null, Set.of(peanutAllergen), Set.of(), Set.of()));

        mvc.perform(get("/api/v1/recipes/search").param("q", "pho").param("showHidden", "true")
                        .header("X-Dev-User-Id", "5"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.dietary.applied").value(false))
                .andExpect(jsonPath("$.dietary.showingHidden").value(true))
                .andExpect(jsonPath("$.dietary.hiddenCount").value(1))
                .andExpect(jsonPath("$.items[?(@.id == " + withPeanut + ")].conflicts[0].type").value(contains("ALLERGEN")))
                .andExpect(jsonPath("$.items[?(@.id == " + withPeanut + ")].conflicts[0].name").value(contains("Đậu phộng")));
    }

    @Test
    void malformedViewerHeaderIs400() throws Exception {
        mvc.perform(get("/api/v1/recipes/search").header("X-Dev-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void optionsListActiveTaxonomy() throws Exception {
        // The database is shared with other test classes: check this fixture's rows only, never switch off theirs.
        fx.tag("METHOD", "Luộc", "luoc");
        fx.tag("EQUIPMENT", "Nồi áp suất", "noi-ap-suat");
        long inactive = fx.cuisine("Ẩm thực đã ẩn", "da-an");
        jdbc.update("UPDATE cuisines SET is_active = false WHERE id = ?", inactive);

        mvc.perform(get("/api/v1/recipes/search/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisines[*].slug").value(hasItems(fx.slug("viet-nam"), fx.slug("nhat-ban"))))
                .andExpect(jsonPath("$.cuisines[*].slug").value(not(hasItem(fx.slug("da-an")))))
                .andExpect(jsonPath("$.categories[*].slug").value(hasItems(fx.slug("mon-chinh"), fx.slug("canh-sup"))))
                .andExpect(jsonPath("$.methods[*].slug").value(hasItem(fx.slug("luoc"))))
                .andExpect(jsonPath("$.methods[*].slug").value(not(hasItem(fx.slug("noi-ap-suat")))))
                .andExpect(jsonPath("$.timeRanges").value(contains("LE15", "LE30", "LE60", "GT60")))
                .andExpect(jsonPath("$.difficulties").value(contains("EASY", "MEDIUM", "HARD")))
                .andExpect(jsonPath("$.diets").value(contains("VEGETARIAN", "VEGAN", "PESCATARIAN")))
                .andExpect(jsonPath("$.sorts").value(contains("RELEVANCE", "NEWEST", "TOP_RATED", "QUICKEST")));
    }

    @Test
    void featuredListsPublishedCards() throws Exception {
        mvc.perform(get("/api/v1/recipes/featured").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].conflicts").isEmpty());
    }

    @Test
    void featuredLimitIsClampedTo20() throws Exception {
        mvc.perform(get("/api/v1/recipes/featured").param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }
}
