package com.wikicook.mealplan.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.dto.PlanItemDto;
import com.wikicook.mealplan.service.exception.InvalidWeekStartException;
import com.wikicook.mealplan.service.exception.RecipeNotAvailableException;
import com.wikicook.mealplan.service.exception.SlotChangedException;
import com.wikicook.mealplan.service.exception.SlotOccupiedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** T010: every meal planner error is a ProblemDetail with a stable {@code code} (research R11). */
@WebMvcTest(controllers = MealPlanExceptionHandlerTest.FakeController.class)
@Import(MealPlanExceptionHandlerTest.FakeController.class)
class MealPlanExceptionHandlerTest {

    static final PlanItemDto PHO = new PlanItemDto(7L, 3, 42L, "Phở bò", null, 45, 2, ItemSource.MANUAL, true,
            DietType.NONE);

    @Autowired
    MockMvc mvc;

    record Body(@Min(1) @Max(20) int servings) {
    }

    @RestController
    static class FakeController {

        @GetMapping("/test/mp/{error}")
        void fail(@PathVariable String error) {
            throw switch (error) {
                case "week" -> new InvalidWeekStartException();
                case "changed" -> new SlotChangedException();
                case "occupied" -> new SlotOccupiedException(PHO);
                case "unavailable" -> new RecipeNotAvailableException();
                default -> new IllegalStateException("boom at com.wikicook.Secret");
            };
        }

        @GetMapping("/test/mp/weeks/{weekStart}")
        void week(@PathVariable @MondayDate @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        }

        @PostMapping("/test/mp/body")
        void body(@Valid @RequestBody Body body) {
        }
    }

    @Test
    void invalidBodyIsValidationError() throws Exception {
        mvc.perform(post("/test/mp/body").contentType(MediaType.APPLICATION_JSON).content("{\"servings\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/test/mp/body").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void weekStartNotAMondayIsInvalidWeekStart() throws Exception { // FR-002
        mvc.perform(get("/test/mp/weeks/2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
        mvc.perform(get("/test/mp/weeks/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
        mvc.perform(get("/test/mp/week"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    @Test
    void slotChangedIs409() throws Exception {
        mvc.perform(get("/test/mp/changed"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SLOT_CHANGED"));
    }

    @Test
    void slotOccupiedIs409WithTheCurrentItem() throws Exception { // US2-3
        mvc.perform(get("/test/mp/occupied"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_OCCUPIED"))
                .andExpect(jsonPath("$.currentItem.id").value(7))
                .andExpect(jsonPath("$.currentItem.version").value(3))
                .andExpect(jsonPath("$.currentItem.recipeName").value("Phở bò"));
    }

    @Test
    void recipeNotAvailableIs422() throws Exception { // FR-010
        mvc.perform(get("/test/mp/unavailable"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_AVAILABLE"));
    }

    @Test
    void unexpectedErrorsHideTheirDetails() throws Exception {
        mvc.perform(get("/test/mp/other"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("Secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("trace"))));
    }
}
