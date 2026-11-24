package com.wikicook.recipe.controller;

import com.wikicook.recipe.dto.RecipeDetailDto;
import com.wikicook.recipe.service.RecipeDetailService;
import com.wikicook.search.port.SearchViewerPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/recipes/{id}}: the detail page, guests included (viewer through 020's {@link SearchViewerPort}). */
@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeDetailController {

    private final RecipeDetailService details;
    private final SearchViewerPort viewer;

    public RecipeDetailController(RecipeDetailService details, SearchViewerPort viewer) {
        this.details = details;
        this.viewer = viewer;
    }

    @GetMapping("/{id:\\d+}")
    public RecipeDetailDto get(@PathVariable long id) {
        return details.get(id, viewer.currentUserId());
    }
}
