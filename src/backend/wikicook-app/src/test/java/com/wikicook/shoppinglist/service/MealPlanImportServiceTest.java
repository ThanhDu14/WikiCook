package com.wikicook.shoppinglist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.domain.ShoppingListStatus;
import com.wikicook.shoppinglist.dto.ImportMealPlanRequest;
import com.wikicook.shoppinglist.dto.ImportMealPlanRequest.Mode;
import com.wikicook.shoppinglist.dto.ShoppingListItemDto;
import com.wikicook.shoppinglist.dto.ShoppingListResponse;
import com.wikicook.shoppinglist.port.MealPlanHandoffPort;
import com.wikicook.shoppinglist.port.MealPlanHandoffPort.MealPlanHandoffDto;
import com.wikicook.shoppinglist.port.MealPlanHandoffPort.MealPlanItemHandoffDto;
import com.wikicook.shoppinglist.port.RecipeDetailPort;
import com.wikicook.shoppinglist.port.RecipeDetailPort.RecipeIngredientImportDto;
import com.wikicook.shoppinglist.repository.ShoppingListItemRepository;
import com.wikicook.shoppinglist.repository.ShoppingListRepository;
import com.wikicook.shoppinglist.repository.UnitRepository;
import com.wikicook.shoppinglist.repository.UserAislePreferenceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** US2 FR-004: a week of the planner becomes one list, scaled per dish and summed per ingredient. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MealPlanImportServiceTest {

    private static final long USER = 7L;
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 23);

    @Mock ShoppingListRepository lists;
    @Mock ShoppingListItemRepository items;
    @Mock UserAislePreferenceRepository aislePreferences;
    @Mock RecipeDetailPort recipes;
    @Mock UnitRepository units;
    @Mock MealPlanHandoffPort mealPlans;

    ShoppingListService service;
    ShoppingList active;

    @BeforeEach
    void setUp() {
        IngredientAggregationService aggregation = new IngredientAggregationService(new UnitConversionService(), units);
        service = new ShoppingListService(lists, items, aislePreferences, recipes, aggregation, units, mealPlans, ids -> Map.of(1L, "https://img/bo-kho.jpg"));
        active = new ShoppingList(USER, "Danh sách mua sắm");
        when(lists.findByUserIdAndStatus(USER, ShoppingListStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(lists.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lists.saveAndFlush(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));
        when(units.findByCode(any())).thenReturn(Optional.empty());
        when(units.findByNameVi(any())).thenReturn(Optional.empty());
    }

    private static RecipeIngredientImportDto ing(long id, String name, String qty, String unit, String aisle) {
        return new RecipeIngredientImportDto(id, name, qty == null ? null : new BigDecimal(qty), 1L, unit, null, aisle);
    }

    private static MealPlanItemHandoffDto dish(long itemId, long recipeId, String title, int base, int servings,
                                               RecipeIngredientImportDto... ingredients) {
        return new MealPlanItemHandoffDto(itemId, recipeId, title, BigDecimal.valueOf(base), BigDecimal.valueOf(servings),
                List.of(ingredients));
    }

    private void plan(MealPlanItemHandoffDto... dishes) {
        when(mealPlans.getMealPlanForShoppingList(USER, WEEK))
                .thenReturn(Optional.of(new MealPlanHandoffDto(WEEK, List.of(dishes), 0)));
    }

    private static ShoppingListItemDto find(ShoppingListResponse response, String name) {
        return response.aisleGroups().stream().flatMap(g -> g.items().stream())
                .filter(i -> name.equals(i.displayName())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("two dishes sharing an ingredient: scaled by their servings and summed into one row with two sources")
    void scalesAndSums() {
        plan(dish(101, 1, "Bò kho", 2, 4, ing(10, "Thịt bò", "300", "g", "MEAT_SEAFOOD"), ing(11, "Sả", "3", "cây", null)),
                dish(102, 2, "Bò xào", 4, 4, ing(10, "Thịt bò", "400", "g", "MEAT_SEAFOOD")));

        ShoppingListResponse response = service.importMealPlan(USER,
                new ImportMealPlanRequest(WEEK, Mode.MERGE, null, null));

        ShoppingListItemDto beef = find(response, "Thịt bò");
        assertThat(beef.displayQuantity()).isEqualByComparingTo("1"); // 600 g + 400 g
        assertThat(beef.displayUnit()).isEqualTo("kg");
        assertThat(beef.sourceRecipeCount()).isEqualTo(2);
        assertThat(beef.aisleCategoryId()).isEqualTo(2L); // MEAT_SEAFOOD
        assertThat(find(response, "Sả").displayQuantity()).isEqualByComparingTo("6");
        assertThat(response.recipeCovers()).containsEntry(1L, "https://img/bo-kho.jpg");
        assertThat(active.getItems()).flatExtracting(ShoppingListItem::getSources)
                .extracting("mealPlanItemId").containsExactlyInAnyOrder(101L, 101L, 102L);
    }

    @Test
    @DisplayName("a serving override replaces the dish's servings; excluded ingredients are left out")
    void overridesAndExclusions() {
        plan(dish(101, 1, "Bò kho", 2, 2, ing(10, "Thịt bò", "300", "g", null), ing(11, "Sả", "3", "cây", null)));

        ShoppingListResponse response = service.importMealPlan(USER, new ImportMealPlanRequest(WEEK, Mode.MERGE,
                List.of(new ImportMealPlanRequest.ServingOverride(101L, new BigDecimal("4"))), List.of(11L)));

        assertThat(find(response, "Thịt bò").displayQuantity()).isEqualByComparingTo("600");
        assertThat(response.aisleGroups()).flatExtracting(g -> g.items()).extracting("displayName")
                .doesNotContain("Sả");
    }

    @Test
    @DisplayName("an ingredient without a quantity stays without one instead of becoming 1")
    void keepsMissingQuantity() {
        plan(dish(101, 1, "Canh", 2, 2, ing(20, "Muối", null, "vừa đủ", null)),
                dish(102, 2, "Xào", 2, 2, ing(20, "Muối", null, "vừa đủ", null)));

        ShoppingListResponse response = service.importMealPlan(USER,
                new ImportMealPlanRequest(WEEK, Mode.MERGE, null, null));

        ShoppingListItemDto salt = find(response, "Muối");
        assertThat(salt.displayQuantity()).isNull();
        assertThat(salt.sourceRecipeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("ARCHIVE_AND_NEW archives an active list that has items and fills a new one")
    void archiveAndNew() {
        active.addItem(new ShoppingListItem(active, UUID.randomUUID(), null, "Giấy bạc", null, null, null, 6L));
        plan(dish(101, 1, "Bò kho", 2, 2, ing(10, "Thịt bò", "300", "g", null)));

        ShoppingListResponse response = service.importMealPlan(USER,
                new ImportMealPlanRequest(WEEK, Mode.ARCHIVE_AND_NEW, null, null));

        assertThat(active.getStatus()).isEqualTo(ShoppingListStatus.ARCHIVED);
        assertThat(response.aisleGroups()).flatExtracting(g -> g.items()).extracting("displayName")
                .containsExactly("Thịt bò");
    }

    @Test
    @DisplayName("MERGE keeps what the list already had")
    void mergeKeepsItems() {
        active.addItem(new ShoppingListItem(active, UUID.randomUUID(), null, "Giấy bạc", null, null, null, 6L));
        plan(dish(101, 1, "Bò kho", 2, 2, ing(10, "Thịt bò", "300", "g", null)));

        ShoppingListResponse response = service.importMealPlan(USER,
                new ImportMealPlanRequest(WEEK, Mode.MERGE, null, null));

        assertThat(active.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(response.aisleGroups()).flatExtracting(g -> g.items()).extracting("displayName")
                .containsExactlyInAnyOrder("Giấy bạc", "Thịt bò");
        verify(lists, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a week without dishes is refused with EMPTY_MEAL_PLAN and the list is untouched")
    void emptyWeek() {
        when(mealPlans.getMealPlanForShoppingList(USER, WEEK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importMealPlan(USER, new ImportMealPlanRequest(WEEK, Mode.MERGE, null, null)))
                .isInstanceOf(EmptyMealPlanException.class);
        verify(lists, never()).save(any());
    }
}
