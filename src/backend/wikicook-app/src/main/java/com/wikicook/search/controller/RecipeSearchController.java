package com.wikicook.search.controller;

import com.wikicook.search.dto.FeaturedResponse;
import com.wikicook.search.dto.SearchOptionsDto;
import com.wikicook.search.dto.SearchResponse;
import com.wikicook.search.port.SearchViewerPort;
import com.wikicook.search.service.RawSearchParams;
import com.wikicook.search.service.RecipeSearchService;
import com.wikicook.search.service.SearchCriteriaParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only search over PUBLISHED recipes (020 contracts/openapi.yaml). Query values are taken as plain strings and
 * validated by {@link SearchCriteriaParser}, so a malformed shared link is ignored instead of answered with 400.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@Tag(name = "Recipe search (020)")
public class RecipeSearchController {

    private static final int DEFAULT_FEATURED = 10;

    private final SearchCriteriaParser parser;
    private final RecipeSearchService service;
    private final SearchViewerPort viewer;

    public RecipeSearchController(SearchCriteriaParser parser, RecipeSearchService service, SearchViewerPort viewer) {
        this.parser = parser;
        this.service = service;
        this.viewer = viewer;
    }

    @GetMapping("/search")
    @Operation(operationId = "searchRecipes", summary = "Search and filter published recipes")
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) List<String> cuisine,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> difficulty,
            @RequestParam(required = false) List<String> method,
            @RequestParam(required = false) String diet,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String showHidden) {
        RawSearchParams raw = new RawSearchParams(q, time, cuisine, category, difficulty, method, diet, sort, page,
                showHidden);
        return service.search(parser.parse(raw), viewer.currentUserId());
    }

    @GetMapping("/search/options")
    @Operation(operationId = "getSearchOptions", summary = "Filter options for the filter panel and chips")
    public SearchOptionsDto options() {
        return service.options();
    }

    @GetMapping("/featured")
    @Operation(operationId = "getFeaturedRecipes", summary = "Home page featured recipes")
    public FeaturedResponse featured(@RequestParam(required = false) String limit) {
        return service.featured(parseLimit(limit));
    }

    private static int parseLimit(String value) {
        if (value == null) {
            return DEFAULT_FEATURED;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return DEFAULT_FEATURED;
        }
    }
}
