package com.wikicook.shoppinglist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.shoppinglist.dto.AisleGroupDto;
import com.wikicook.shoppinglist.dto.CreateCustomItemRequest;
import com.wikicook.shoppinglist.dto.ImportRecipeRequest;
import com.wikicook.shoppinglist.dto.ItemProvenanceDto;
import com.wikicook.shoppinglist.dto.ShoppingListItemDto;
import com.wikicook.shoppinglist.dto.ShoppingListResponse;
import com.wikicook.shoppinglist.dto.UpdateTitleRequest;
import com.wikicook.shoppinglist.service.CapacityExceededException;
import com.wikicook.shoppinglist.service.ShoppingListNotFoundException;
import com.wikicook.shoppinglist.service.ShoppingListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShoppingListController.class)
@Import(ShoppingListExceptionHandler.class)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShoppingListService shoppingListService;

    private final Long userId = 100L;

    private ShoppingListResponse createSampleResponse(Long id, String title, String status) {
        return new ShoppingListResponse(
                id,
                title,
                status,
                0,
                0,
                0,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("GET /api/v1/shopping-lists/active returns active shopping list")
    void getActiveShoppingList_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Danh sách mua sắm 29/10", "ACTIVE");
        when(shoppingListService.getActiveShoppingList(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shopping-lists/active")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Danh sách mua sắm 29/10"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/shopping-lists/active updates title")
    void updateTitle_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Tiệc sinh nhật cuối tuần", "ACTIVE");
        when(shoppingListService.renameActiveShoppingList(eq(userId), eq("Tiệc sinh nhật cuối tuần"))).thenReturn(response);

        UpdateTitleRequest request = new UpdateTitleRequest("Tiệc sinh nhật cuối tuần");

        mockMvc.perform(patch("/api/v1/shopping-lists/active")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiệc sinh nhật cuối tuần"));
    }

    @Test
    @DisplayName("POST /api/v1/shopping-lists/active/archive archives active and returns new active list")
    void archiveActiveShoppingList_returns201() throws Exception {
        ShoppingListResponse response = createSampleResponse(2L, "Danh sách mua sắm mới", "ACTIVE");
        when(shoppingListService.archiveActiveShoppingList(userId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shopping-lists/active/archive")
                        .header("X-User-Id", userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/shopping-lists returns archived lists history")
    void getArchivedShoppingLists_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Chuyến đi cũ", "ARCHIVED");
        when(shoppingListService.getArchivedLists(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/shopping-lists")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("GET /api/v1/shopping-lists/{id} returns list detail")
    void getShoppingListById_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Chuyến đi cũ", "ARCHIVED");
        when(shoppingListService.getShoppingListById(userId, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shopping-lists/1")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/shopping-lists/{id} returns 404 when not found")
    void getShoppingListById_notFound_returns404() throws Exception {
        when(shoppingListService.getShoppingListById(userId, 999L))
                .thenThrow(new ShoppingListNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/shopping-lists/999")
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Not found"));
    }

    @Test
    @DisplayName("POST /api/v1/shopping-lists/{id}/restore restores archived list")
    void restoreArchivedShoppingList_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Danh sách phục hồi", "ACTIVE");
        when(shoppingListService.restoreArchivedList(userId, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shopping-lists/1/restore")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/shopping-lists/items adds custom item")
    void createCustomItem_returns201() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Active List", "ACTIVE");
        when(shoppingListService.createCustomItem(eq(userId), any())).thenReturn(response);

        CreateCustomItemRequest request = new CreateCustomItemRequest(
                UUID.randomUUID(), "Dầu ăn", new BigDecimal("1"), "chai", 6L);

        mockMvc.perform(post("/api/v1/shopping-lists/items")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/shopping-lists/items returns 422 when capacity exceeded")
    void createCustomItem_capacityExceeded_returns422() throws Exception {
        when(shoppingListService.createCustomItem(eq(userId), any()))
                .thenThrow(new CapacityExceededException("Capacity limit reached"));

        CreateCustomItemRequest request = new CreateCustomItemRequest(
                UUID.randomUUID(), "Dầu ăn", new BigDecimal("1"), "chai", 6L);

        mockMvc.perform(post("/api/v1/shopping-lists/items")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Capacity limit reached"));
    }

    @Test
    @DisplayName("DELETE /api/v1/shopping-lists/items/{clientUuid} deletes item")
    void deleteShoppingListItem_returns204() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        doNothing().when(shoppingListService).deleteItem(userId, itemUuid);

        mockMvc.perform(delete("/api/v1/shopping-lists/items/" + itemUuid)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/shopping-lists/import/recipe imports recipe ingredients")
    void importRecipeIngredients_returns200() throws Exception {
        ShoppingListResponse response = createSampleResponse(1L, "Active List", "ACTIVE");
        when(shoppingListService.importRecipeIngredients(eq(userId), any())).thenReturn(response);

        ImportRecipeRequest request = new ImportRecipeRequest(1L, new BigDecimal("4"), null);

        mockMvc.perform(post("/api/v1/shopping-lists/import/recipe")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/shopping-lists/items/{clientUuid}/provenance returns provenance items")
    void getItemProvenance_returns200() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        ItemProvenanceDto p = new ItemProvenanceDto(1L, 10L, null, "Bò kho", new BigDecimal("600"), "g");
        when(shoppingListService.getItemProvenance(userId, itemUuid)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/v1/shopping-lists/items/" + itemUuid + "/provenance")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipeTitleSnapshot").value("Bò kho"))
                .andExpect(jsonPath("$[0].contributedQuantity").value(600));
    }
}
