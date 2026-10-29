package com.wikicook.shoppinglist.service;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListItem;
import com.wikicook.shoppinglist.domain.ShoppingListItemSource;
import com.wikicook.shoppinglist.domain.ShoppingListStatus;
import com.wikicook.shoppinglist.dto.*;
import com.wikicook.shoppinglist.port.RecipeDetailPort;
import com.wikicook.shoppinglist.repository.ShoppingListItemRepository;
import com.wikicook.shoppinglist.repository.ShoppingListRepository;
import com.wikicook.shoppinglist.repository.UserAislePreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private ShoppingListItemRepository shoppingListItemRepository;

    @Mock
    private UserAislePreferenceRepository userAislePreferenceRepository;

    @Mock
    private RecipeDetailPort recipeDetailPort;

    @Mock
    private IngredientAggregationService ingredientAggregationService;

    @InjectMocks
    private ShoppingListService shoppingListService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 100L;
    }

    @Test
    @DisplayName("US1 FR-001: getActiveShoppingList creates a default active list when none exists")
    void getActiveShoppingList_createsDefaultWhenNoneExists() {
        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.empty());

        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(invocation -> {
            ShoppingList list = invocation.getArgument(0);
            list.setId(1L);
            return list;
        });

        ShoppingListResponse response = shoppingListService.getActiveShoppingList(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).startsWith("Danh sách mua sắm");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.totalItems()).isEqualTo(0);
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    @DisplayName("US1 FR-001: getActiveShoppingList returns existing active list when found")
    void getActiveShoppingList_returnsExistingWhenFound() {
        ShoppingList activeList = new ShoppingList(userId, "Danh sách tiệc sinh nhật");
        activeList.setId(2L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(activeList));

        ShoppingListResponse response = shoppingListService.getActiveShoppingList(userId);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.title()).isEqualTo("Danh sách tiệc sinh nhật");
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(shoppingListRepository, never()).save(any(ShoppingList.class));
    }

    @Test
    @DisplayName("US1: renameActiveShoppingList updates active list title")
    void renameActiveShoppingList_updatesTitle() {
        ShoppingList activeList = new ShoppingList(userId, "Tên cũ");
        activeList.setId(3L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(activeList));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(activeList);

        ShoppingListResponse response = shoppingListService.renameActiveShoppingList(userId, "Đi chợ cuối tuần");

        assertThat(response.title()).isEqualTo("Đi chợ cuối tuần");
        assertThat(activeList.getTitle()).isEqualTo("Đi chợ cuối tuần");
        verify(shoppingListRepository).save(activeList);
    }

    @Test
    @DisplayName("US1 FR-025: createCustomItem throws CapacityExceededException when capacity >= 200")
    void createCustomItem_throwsCapacityExceededException_whenLimitReached() {
        ShoppingList activeList = new ShoppingList(userId, "Active List");
        activeList.setId(4L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(activeList));
        when(shoppingListItemRepository.countByShoppingListId(4L)).thenReturn(200L);

        CreateCustomItemRequest request = new CreateCustomItemRequest(
                UUID.randomUUID(), "Nước rửa chén", BigDecimal.ONE, "chai", null);

        assertThatThrownBy(() -> shoppingListService.createCustomItem(userId, request))
                .isInstanceOf(CapacityExceededException.class)
                .hasMessageContaining("200");
    }

    @Test
    @DisplayName("US1 FR-012: createCustomItem adds new custom item defaulting to General aisle")
    void createCustomItem_addsNewCustomItem_withGeneralAisle() {
        ShoppingList activeList = new ShoppingList(userId, "Active List");
        activeList.setId(5L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(activeList));
        when(shoppingListItemRepository.countByShoppingListId(5L)).thenReturn(0L);
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID clientUuid = UUID.randomUUID();
        CreateCustomItemRequest request = new CreateCustomItemRequest(
                clientUuid, "Khăn giấy", new BigDecimal("2"), "cuộn", null);

        ShoppingListResponse response = shoppingListService.createCustomItem(userId, request);

        assertThat(response).isNotNull();
        assertThat(activeList.getItems()).hasSize(1);
        ShoppingListItem savedItem = activeList.getItems().get(0);
        assertThat(savedItem.getCustomName()).isEqualTo("Khăn giấy");
        assertThat(savedItem.getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(savedItem.getAisleCategoryId()).isEqualTo(6L); // General
        assertThat(savedItem.getClientUuid()).isEqualTo(clientUuid);
        verify(shoppingListRepository).save(activeList);
    }

    @Test
    @DisplayName("US1 FR-022: archiveActiveShoppingList archives active and creates new blank active")
    void archiveActiveShoppingList_archivesCurrentAndCreatesNew() {
        ShoppingList currentActive = new ShoppingList(userId, "Danh sách cần lưu trữ");
        currentActive.setId(6L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(currentActive));

        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(invocation -> {
            ShoppingList list = invocation.getArgument(0);
            if (list.getStatus() == ShoppingListStatus.ACTIVE && list != currentActive) {
                list.setId(7L);
            }
            return list;
        });

        ShoppingListResponse response = shoppingListService.archiveActiveShoppingList(userId);

        assertThat(currentActive.getStatus()).isEqualTo(ShoppingListStatus.ARCHIVED);
        assertThat(currentActive.getArchivedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.totalItems()).isEqualTo(0);
    }

    @Test
    @DisplayName("US1 FR-022: restoreArchivedList resets isChecked=false and isCleared=false")
    void restoreArchivedList_resetsCheckedAndCleared() {
        ShoppingList currentActive = new ShoppingList(userId, "Active cũ");
        currentActive.setId(8L);

        ShoppingList archived = new ShoppingList(userId, "Danh sách cũ");
        archived.setId(9L);
        archived.setStatus(ShoppingListStatus.ARCHIVED);

        ShoppingListItem item = new ShoppingListItem(archived, UUID.randomUUID(), 10L, "Thịt heo",
                new BigDecimal("500"), 1L, "g", 2L);
        item.setChecked(true);
        item.setCleared(true);
        item.setClearedAt(Instant.now());
        archived.addItem(item);

        when(shoppingListRepository.findByIdAndUserId(9L, userId)).thenReturn(Optional.of(archived));
        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(currentActive));

        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));

        ShoppingListResponse response = shoppingListService.restoreArchivedList(userId, 9L);

        assertThat(currentActive.getStatus()).isEqualTo(ShoppingListStatus.ARCHIVED);
        assertThat(archived.getStatus()).isEqualTo(ShoppingListStatus.ACTIVE);
        assertThat(item.isChecked()).isFalse();
        assertThat(item.isCleared()).isFalse();
        assertThat(item.getClearedAt()).isNull();
    }

    @Test
    @DisplayName("US1: deleteItem removes item by clientUuid")
    void deleteItem_removesItem() {
        ShoppingList activeList = new ShoppingList(userId, "Active List");
        activeList.setId(10L);

        UUID itemUuid = UUID.randomUUID();
        ShoppingListItem item = new ShoppingListItem(activeList, itemUuid, null, "Món thừa",
                BigDecimal.ONE, 1L, "cái", 6L);
        activeList.addItem(item);

        when(shoppingListItemRepository.findByClientUuid(itemUuid)).thenReturn(Optional.of(item));

        shoppingListService.deleteItem(userId, itemUuid);

        verify(shoppingListItemRepository).delete(item);
    }

    @Test
    @DisplayName("US2: importRecipeIngredients fetches recipe, invokes aggregation engine and saves")
    void importRecipeIngredients_fetchesAndAggregates() {
        ShoppingList activeList = new ShoppingList(userId, "Active List");
        activeList.setId(10L);

        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(activeList));

        RecipeDetailPort.RecipeImportDto recipe = new RecipeDetailPort.RecipeImportDto(
                1L, "Bò kho", new BigDecimal("2"), List.of()
        );
        when(recipeDetailPort.getRecipeForImport(1L)).thenReturn(Optional.of(recipe));
        when(shoppingListRepository.save(activeList)).thenReturn(activeList);

        ImportRecipeRequest request = new ImportRecipeRequest(1L, new BigDecimal("4"), null);
        ShoppingListResponse response = shoppingListService.importRecipeIngredients(userId, request);

        assertThat(response).isNotNull();
        verify(ingredientAggregationService).aggregateRecipe(eq(activeList), eq(recipe), eq(new BigDecimal("4")), any());
        verify(shoppingListRepository).save(activeList);
    }

    @Test
    @DisplayName("US2: importRecipeIngredients throws ShoppingListNotFoundException when recipe not found")
    void importRecipeIngredients_throwsWhenRecipeNotFound() {
        when(shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE))
                .thenReturn(Optional.of(new ShoppingList(userId, "Active List")));
        when(recipeDetailPort.getRecipeForImport(999L)).thenReturn(Optional.empty());

        ImportRecipeRequest request = new ImportRecipeRequest(999L, new BigDecimal("2"), null);
        assertThatThrownBy(() -> shoppingListService.importRecipeIngredients(userId, request))
                .isInstanceOf(ShoppingListNotFoundException.class);
    }

    @Test
    @DisplayName("US2: getItemProvenance returns provenance list for item")
    void getItemProvenance_returnsList() {
        ShoppingList activeList = new ShoppingList(userId, "Active List");
        UUID itemUuid = UUID.randomUUID();
        ShoppingListItem item = new ShoppingListItem(activeList, itemUuid, 1L, "Thịt bò", new BigDecimal("1"), 1L, "kg", 2L);
        ShoppingListItemSource source = new ShoppingListItemSource(item, 10L, null, "Bò kho", new BigDecimal("600"), 1L, "g", 4);
        item.addSource(source);

        when(shoppingListItemRepository.findByClientUuid(itemUuid)).thenReturn(Optional.of(item));

        List<ItemProvenanceDto> provenance = shoppingListService.getItemProvenance(userId, itemUuid);

        assertThat(provenance).hasSize(1);
        assertThat(provenance.get(0).recipeTitleSnapshot()).isEqualTo("Bò kho");
        assertThat(provenance.get(0).contributedQuantity()).isEqualByComparingTo(new BigDecimal("600"));
    }
}
