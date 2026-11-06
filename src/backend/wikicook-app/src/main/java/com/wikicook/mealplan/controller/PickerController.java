package com.wikicook.mealplan.controller;

import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.dto.PickerPageDto;
import com.wikicook.mealplan.service.PickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recipe search of the slot picker (FR-007, FR-007a, FR-007b). */
@RestController
@Tag(name = "picker")
public class PickerController {

    private final CurrentUserId currentUser;
    private final PickerService picker;

    public PickerController(CurrentUserId currentUser, PickerService picker) {
        this.currentUser = currentUser;
        this.picker = picker;
    }

    @Operation(operationId = "searchPickerRecipes", summary = "Tìm công thức cho hộp chọn của một ô")
    @GetMapping("/api/v1/meal-plans/picker/recipes")
    public PickerPageDto search(@RequestParam Meal meal,
                                @RequestParam(required = false) @Size(max = 100) String q,
                                @RequestParam(defaultValue = "ALL") PickerService.Scope scope,
                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return picker.search(currentUser.get(), meal, q, scope, page, size);
    }
}
