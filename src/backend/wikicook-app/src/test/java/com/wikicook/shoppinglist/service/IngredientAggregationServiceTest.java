package com.wikicook.shoppinglist.service;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeImportDto;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngredientAggregationServiceTest {

    private UnitConversionService unitConversionService;
    private IngredientAggregationService ingredientAggregationService;

    @BeforeEach
    void setUp() {
        unitConversionService = new UnitConversionService();
        ingredientAggregationService = new IngredientAggregationService(unitConversionService);
    }

    @Test
    @DisplayName("US2 Scenario 1 & 2: scales quantities and consolidates identical ingredients (300g x 2 + 400g = 1 kg)")
    void aggregateRecipeIngredients_consolidatesAndConvertsUnits() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");

        // First recipe: Recipe A (Thịt bò 300g for base 2 servings, scaled to 4 servings -> 600g)
        RecipeImportDto recipeA = new RecipeImportDto(
                10L,
                "Bò kho",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(
                                1L, "Thịt bò", new BigDecimal("300"), 1L, "g", 2L, "MEAT_SEAFOOD"
                        )
                )
        );

        ingredientAggregationService.aggregateRecipe(activeList, recipeA, new BigDecimal("4"), Collections.emptySet());

        assertThat(activeList.getItems()).hasSize(1);
        ShoppingListItem beefItem = activeList.getItems().get(0);
        assertThat(beefItem.getQuantity()).isEqualByComparingTo(new BigDecimal("600"));
        assertThat(beefItem.getUnitName()).isEqualTo("g");
        assertThat(beefItem.getSources()).hasSize(1);
        assertThat(beefItem.getSources().get(0).getRecipeTitleSnapshot()).isEqualTo("Bò kho");

        // Second recipe: Recipe B (Thịt bò 400g for base 4 servings, scaled to 4 servings -> 400g)
        RecipeImportDto recipeB = new RecipeImportDto(
                20L,
                "Bò xào cần tây",
                new BigDecimal("4"),
                List.of(
                        new RecipeIngredientImportDto(
                                1L, "Thịt bò", new BigDecimal("400"), 1L, "g", 2L, "MEAT_SEAFOOD"
                        )
                )
        );

        ingredientAggregationService.aggregateRecipe(activeList, recipeB, new BigDecimal("4"), Collections.emptySet());

        // Assert consolidation into 1 kg Thịt bò with 2 provenance records
        assertThat(activeList.getItems()).hasSize(1);
        assertThat(beefItem.getQuantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(beefItem.getUnitName()).isEqualTo("kg");
        assertThat(beefItem.getSources()).hasSize(2);
        assertThat(beefItem.getSources().get(0).getRecipeTitleSnapshot()).isEqualTo("Bò kho");
        assertThat(beefItem.getSources().get(1).getRecipeTitleSnapshot()).isEqualTo("Bò xào cần tây");
    }

    @Test
    @DisplayName("US2 Scenario 4: keeps incompatible units on separate rows (3 quả chanh vs 50 ml nước cốt chanh)")
    void aggregateRecipeIngredients_incompatibleUnits_remainSeparate() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");

        RecipeImportDto recipe1 = new RecipeImportDto(
                10L,
                "Nước chanh tươi",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(
                                5L, "Chanh", new BigDecimal("3"), 5L, "quả", 1L, "PRODUCE"
                        )
                )
        );

        RecipeImportDto recipe2 = new RecipeImportDto(
                20L,
                "Sốt chanh dây",
                new BigDecimal("2"),
                List.of(
                        new RecipeIngredientImportDto(
                                5L, "Chanh", new BigDecimal("50"), 2L, "ml", 1L, "PRODUCE"
                        )
                )
        );

        ingredientAggregationService.aggregateRecipe(activeList, recipe1, new BigDecimal("2"), Collections.emptySet());
        ingredientAggregationService.aggregateRecipe(activeList, recipe2, new BigDecimal("2"), Collections.emptySet());

        assertThat(activeList.getItems()).hasSize(2);
        assertThat(activeList.getItems()).anySatisfy(item -> {
            assertThat(item.getIngredientId()).isEqualTo(5L);
            assertThat(item.getUnitName()).isEqualTo("quả");
            assertThat(item.getQuantity()).isEqualByComparingTo(new BigDecimal("3"));
        });
        assertThat(activeList.getItems()).anySatisfy(item -> {
            assertThat(item.getIngredientId()).isEqualTo(5L);
            assertThat(item.getUnitName()).isEqualTo("ml");
            assertThat(item.getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
        });
    }

    @Test
    @DisplayName("US2 Scenario 7: rounds up discrete countable units (0.5 quả trứng -> 1 quả trứng)")
    void aggregateRecipeIngredients_roundsUpDiscreteUnits() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");

        RecipeImportDto recipe = new RecipeImportDto(
                10L,
                "Bánh flan",
                new BigDecimal("4"),
                List.of(
                        new RecipeIngredientImportDto(
                                8L, "Trứng gà", new BigDecimal("2"), 8L, "quả", 3L, "DAIRY_EGGS"
                        )
                )
        );

        // Scale from 4 to 1 serving -> 2 * (1/4) = 0.5 quả -> rounds up to 1 quả
        ingredientAggregationService.aggregateRecipe(activeList, recipe, new BigDecimal("1"), Collections.emptySet());

        assertThat(activeList.getItems()).hasSize(1);
        ShoppingListItem eggItem = activeList.getItems().get(0);
        assertThat(eggItem.getQuantity()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(eggItem.getUnitName()).isEqualTo("quả");
    }

    @Test
    @DisplayName("US2 Scenario 8: filters out excluded basic staple ingredients")
    void aggregateRecipeIngredients_filtersExcludedStaples() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");

        RecipeImportDto recipe = new RecipeImportDto(
                10L,
                "Canh chua cá lóc",
                new BigDecimal("4"),
                List.of(
                        new RecipeIngredientImportDto(10L, "Cá lóc", new BigDecimal("500"), 1L, "g", 2L, "MEAT_SEAFOOD"),
                        new RecipeIngredientImportDto(99L, "Muối", new BigDecimal("5"), 1L, "g", 4L, "PANTRY_SPICES"),
                        new RecipeIngredientImportDto(100L, "Đường", new BigDecimal("10"), 1L, "g", 4L, "PANTRY_SPICES")
                )
        );

        // Exclude Muối (99) and Đường (100)
        ingredientAggregationService.aggregateRecipe(activeList, recipe, new BigDecimal("4"), Set.of(99L, 100L));

        assertThat(activeList.getItems()).hasSize(1);
        assertThat(activeList.getItems().get(0).getCustomName()).isEqualTo("Cá lóc");
    }

    @Test
    @DisplayName("US2 FR-016: unchecks previously checked item if additional quantity is merged")
    void aggregateRecipeIngredients_unchecksPurchasedItemOnMerge() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");

        RecipeImportDto recipe1 = new RecipeImportDto(
                10L, "Món 1", new BigDecimal("2"),
                List.of(new RecipeIngredientImportDto(1L, "Hành tím", new BigDecimal("100"), 1L, "g", 1L, "PRODUCE"))
        );
        ingredientAggregationService.aggregateRecipe(activeList, recipe1, new BigDecimal("2"), Collections.emptySet());

        ShoppingListItem item = activeList.getItems().get(0);
        item.setChecked(true); // User checked off item in supermarket

        RecipeImportDto recipe2 = new RecipeImportDto(
                20L, "Món 2", new BigDecimal("2"),
                List.of(new RecipeIngredientImportDto(1L, "Hành tím", new BigDecimal("100"), 1L, "g", 1L, "PRODUCE"))
        );
        ingredientAggregationService.aggregateRecipe(activeList, recipe2, new BigDecimal("2"), Collections.emptySet());

        assertThat(item.isChecked()).isFalse(); // MUST be unchecked per FR-016
        assertThat(item.getQuantity()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("US2 FR-025: throws CapacityExceededException when item limit exceeds 200")
    void aggregateRecipeIngredients_throwsCapacityExceeded() {
        ShoppingList activeList = new ShoppingList(100L, "Danh sách mua sắm");
        for (int i = 0; i < 200; i++) {
            activeList.addItem(new ShoppingListItem(activeList, UUID.randomUUID(), (long) (1000 + i), "Món " + i,
                    BigDecimal.ONE, 1L, "g", 6L));
        }

        RecipeImportDto recipe = new RecipeImportDto(
                10L, "Món mới", new BigDecimal("2"),
                List.of(new RecipeIngredientImportDto(9999L, "Nguyên liệu mới", new BigDecimal("100"), 1L, "g", 1L, "PRODUCE"))
        );

        assertThatThrownBy(() -> ingredientAggregationService.aggregateRecipe(activeList, recipe, new BigDecimal("2"), Collections.emptySet()))
                .isInstanceOf(CapacityExceededException.class);
    }
}
