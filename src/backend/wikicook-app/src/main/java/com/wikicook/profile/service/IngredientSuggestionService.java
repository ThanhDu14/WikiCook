package com.wikicook.profile.service;

import com.wikicook.profile.dto.IngredientSuggestionDto;
import com.wikicook.profile.port.IngredientCatalogPort;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Suggestions while the person types an ingredient to exclude (FR-011, research R6). Under two characters nothing is
 * asked of the database. The ranking and the escaping of {@code % _ \} live in the catalogue port.
 */
@Service
public class IngredientSuggestionService {

    static final int MIN_LENGTH = 2;
    static final int LIMIT = 10;

    private final IngredientCatalogPort catalogue;

    public IngredientSuggestionService(IngredientCatalogPort catalogue) {
        this.catalogue = catalogue;
    }

    public List<IngredientSuggestionDto> suggest(String text) {
        String query = text == null ? "" : text.strip();
        if (query.codePointCount(0, query.length()) < MIN_LENGTH) {
            return List.of();
        }
        return catalogue.suggest(query, LIMIT).stream()
                .map(name -> new IngredientSuggestionDto(name.id(), name.name())).toList();
    }
}
