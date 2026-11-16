package com.wikicook.mealplan.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A 030 controller (package {@code com.wikicook.mealplan}) with a validated body, to prove that 010's handlers leave
 * its validation errors to {@code MealPlanExceptionHandler} (research R15). A {@code @TestConfiguration} controller, added with {@code @Import}, never scanned.
 */
@TestConfiguration(proxyBeanMethods = false)
@Controller
@RequestMapping("/api/v1/meal-probe")
@ResponseBody
public class MealPlanValidationProbe {

    public record Body(@NotBlank String name) {
    }

    @PostMapping
    public Map<String, Object> post(@Valid @RequestBody Body body) {
        return Map.of("ok", true);
    }
}
