package com.wikicook.profile.controller;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.profile.dto.IngredientSuggestionDto;
import com.wikicook.profile.service.IngredientSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /ingredients/suggestions}: what the exclusion picker offers while the person types (FR-011). */
@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientSuggestionController {

    private final CurrentUser currentUser;
    private final IngredientSuggestionService suggestions;

    public IngredientSuggestionController(CurrentUser currentUser, IngredientSuggestionService suggestions) {
        this.currentUser = currentUser;
        this.suggestions = suggestions;
    }

    @Operation(operationId = "suggestIngredients",
            summary = "Gợi ý nguyên liệu APPROVED, bỏ dấu và không phân biệt hoa thường; dưới 2 ký tự trả danh sách rỗng")
    @GetMapping("/suggestions")
    public List<IngredientSuggestionDto> suggest(@RequestParam @Size(max = 50, message = "TOO_LONG") String q) {
        currentUser.id(); // the catalogue is for people who are building a profile, not for guests
        return suggestions.suggest(q);
    }
}
