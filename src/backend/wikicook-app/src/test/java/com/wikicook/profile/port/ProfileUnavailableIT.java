package com.wikicook.profile.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.common.diet.DietaryProfileUnavailableException;
import com.wikicook.profile.service.DietaryProfileReader;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import com.wikicook.support.HeaderCurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T034a (FR-026a, research R8, R17): when the dietary profile can not be read, the three routes that need it answer
 * 503 PROFILE_UNAVAILABLE. None of them picks a dish on its own, and none leaks the cause.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({HeaderCurrentUser.class, AuthTestSupport.class})
class ProfileUnavailableIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthTestSupport support;

    @MockitoBean
    DietaryProfileReader reader;

    long user;

    @BeforeEach
    void setUp() {
        user = support.member();
        when(reader.read(anyLong())).thenThrow(new DietaryProfileUnavailableException("jdbc:postgresql://secret-host/db is down",
                new RuntimeException("connection refused")));
    }

    private void assertUnavailable(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        String body = result.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROFILE_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("jdbc").doesNotContain("secret-host").doesNotContain("connection refused")
                .doesNotContain("Exception").doesNotContain("com.wikicook");
    }

    @Test
    @DisplayName("FR-026a: GET /me/dietary-profile is 503 PROFILE_UNAVAILABLE, not an empty profile")
    void myDietaryProfile() throws Exception {
        assertUnavailable(mvc.perform(get("/api/v1/me/dietary-profile").header(HeaderCurrentUser.HEADER, user)));
    }

    @Test
    @DisplayName("FR-026a: the meal planner picker is 503 and shows no recipe")
    void picker() throws Exception {
        assertUnavailable(mvc.perform(get("/api/v1/meal-plans/picker/recipes").param("meal", "LUNCH")
                .header(HeaderCurrentUser.HEADER, user)));
    }

    @Test
    @DisplayName("FR-026a: the AI planner options are 503, so no dish is chosen without the allergies")
    void aiPlannerOptions() throws Exception {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);

        assertUnavailable(mvc.perform(get("/api/v1/ai-planner/weeks/{weekStart}/options", monday)
                .header(HeaderCurrentUser.HEADER, user)));
    }
}
