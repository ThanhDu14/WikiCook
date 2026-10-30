package com.wikicook.shoppinglist.controller;

import com.wikicook.shoppinglist.dto.CreateCustomItemRequest;
import com.wikicook.shoppinglist.dto.ImportRecipeRequest;
import com.wikicook.shoppinglist.dto.ItemProvenanceDto;
import com.wikicook.shoppinglist.dto.ShoppingListResponse;
import com.wikicook.shoppinglist.dto.UpdateTitleRequest;
import com.wikicook.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping("/active")
    public ResponseEntity<ShoppingListResponse> getActiveShoppingList(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        ShoppingListResponse response = shoppingListService.getActiveShoppingList(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/active")
    public ResponseEntity<ShoppingListResponse> renameActiveShoppingList(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody UpdateTitleRequest request) {
        ShoppingListResponse response = shoppingListService.renameActiveShoppingList(userId, request.title());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/active/archive")
    public ResponseEntity<ShoppingListResponse> archiveActiveShoppingList(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        ShoppingListResponse response = shoppingListService.archiveActiveShoppingList(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"", "/history"})
    public ResponseEntity<List<ShoppingListResponse>> getArchivedShoppingLists(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        List<ShoppingListResponse> responses = shoppingListService.getArchivedLists(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getShoppingListById(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long id) {
        ShoppingListResponse response = shoppingListService.getShoppingListById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ShoppingListResponse> restoreArchivedShoppingList(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long id) {
        ShoppingListResponse response = shoppingListService.restoreArchivedList(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/items", "/active/items"})
    public ResponseEntity<ShoppingListResponse> createCustomItem(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody CreateCustomItemRequest request) {
        ShoppingListResponse response = shoppingListService.createCustomItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/items/{clientUuid}", "/active/items/{clientUuid}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteShoppingListItem(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable UUID clientUuid) {
        shoppingListService.deleteItem(userId, clientUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/import/recipe", "/active/import/recipe"})
    public ResponseEntity<ShoppingListResponse> importRecipeIngredients(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody ImportRecipeRequest request) {
        ShoppingListResponse response = shoppingListService.importRecipeIngredients(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/items/{clientUuid}/provenance", "/active/items/{clientUuid}/provenance"})
    public ResponseEntity<List<ItemProvenanceDto>> getItemProvenance(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable UUID clientUuid) {
        List<ItemProvenanceDto> provenance = shoppingListService.getItemProvenance(userId, clientUuid);
        return ResponseEntity.ok(provenance);
    }
}
