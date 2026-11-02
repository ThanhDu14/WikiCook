package com.wikicook.shoppinglist.port;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Seed data adapter for RecipeDetailPort providing decoupled standalone recipe data for US1/US2 testing and manual verification.
 */
public class SeedRecipeDetailAdapter implements RecipeDetailPort {

    private static final Logger log = LoggerFactory.getLogger(SeedRecipeDetailAdapter.class);

    private final Map<Long, RecipeImportDto> seedRecipes = new HashMap<>();

    public SeedRecipeDetailAdapter() {
        // Standard Unit IDs: 1=g, 2=kg, 3=ml, 4=l, 5=quả, 6=củ, 7=cây
        seedRecipes.put(1L, new RecipeImportDto(
                1L,
                "Bò kho",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(1L, "Thịt bò", new BigDecimal("300"), 1L, "g", 2L, "MEAT_SEAFOOD"),
                        new RecipeIngredientImportDto(2L, "Cà rốt", new BigDecimal("2"), 6L, "củ", 1L, "PRODUCE"),
                        new RecipeIngredientImportDto(3L, "Sả", new BigDecimal("3"), 7L, "cây", 1L, "PRODUCE")
                )
        ));

        seedRecipes.put(2L, new RecipeImportDto(
                2L,
                "Bò xào cần tây",
                new BigDecimal("4"),
                List.of(
                        new RecipeIngredientImportDto(1L, "Thịt bò", new BigDecimal("400"), 1L, "g", 2L, "MEAT_SEAFOOD"),
                        new RecipeIngredientImportDto(4L, "Cần tây", new BigDecimal("200"), 1L, "g", 1L, "PRODUCE"),
                        new RecipeIngredientImportDto(5L, "Tỏi", new BigDecimal("1"), 6L, "củ", 1L, "PRODUCE")
                )
        ));

        seedRecipes.put(3L, new RecipeImportDto(
                3L,
                "Nước chanh tươi",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(6L, "Chanh", new BigDecimal("3"), 5L, "quả", 1L, "PRODUCE"),
                        new RecipeIngredientImportDto(7L, "Đường", new BigDecimal("50"), 1L, "g", 4L, "PANTRY_SPICES")
                )
        ));

        seedRecipes.put(4L, new RecipeImportDto(
                4L,
                "Sốt chanh dây",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(6L, "Chanh", new BigDecimal("50"), 3L, "ml", 1L, "PRODUCE"),
                        new RecipeIngredientImportDto(7L, "Đường", new BigDecimal("30"), 1L, "g", 4L, "PANTRY_SPICES")
                )
        ));

        seedRecipes.put(5L, new RecipeImportDto(
                5L,
                "Bánh flan",
                new BigDecimal("4"),
                List.of(
                        new RecipeIngredientImportDto(8L, "Trứng gà", new BigDecimal("2"), 5L, "quả", 3L, "DAIRY_EGGS"),
                        new RecipeIngredientImportDto(9L, "Sữa tươi", new BigDecimal("500"), 3L, "ml", 3L, "DAIRY_EGGS")
                )
        ));
    }

    @Override
    public Optional<RecipeImportDto> getRecipeForImport(Long recipeId) {
        return Optional.ofNullable(seedRecipes.get(recipeId));
    }

    @PostConstruct
    public void warnIfActive() {
        log.warn("SeedRecipeDetailAdapter is active: serving mock recipe data. Real RecipeDetailPort adapter from module 060 has not been registered.");
    }
}
