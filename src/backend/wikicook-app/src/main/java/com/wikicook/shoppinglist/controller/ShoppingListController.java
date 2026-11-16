package com.wikicook.shoppinglist.controller;

import com.wikicook.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The caller is always {@link CurrentUser#id()} (spec 010): nobody can read or change another user's list by sending a
 * different id, because there is no user id in the request any more.
 */
@RestController
@RequestMapping("/api/v1/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final CurrentUser currentUser;

    public ShoppingListController(ShoppingListService shoppingListService, CurrentUser currentUser) {
        this.shoppingListService = shoppingListService;
        this.currentUser = currentUser;
    }

    @GetMapping("/active")
    public ResponseEntity<ShoppingListResponse> getActiveShoppingList() {
        ShoppingListResponse response = shoppingListService.getActiveShoppingList(currentUser.id());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/active")
    public ResponseEntity<ShoppingListResponse> renameActiveShoppingList(
            @Valid @RequestBody UpdateTitleRequest request) {
        ShoppingListResponse response = shoppingListService.renameActiveShoppingList(currentUser.id(),
                request.title());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/active/archive")
    public ResponseEntity<ShoppingListResponse> archiveActiveShoppingList() {
        ShoppingListResponse response = shoppingListService.archiveActiveShoppingList(currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"", "/history"})
    public ResponseEntity<List<ShoppingListResponse>> getArchivedShoppingLists() {
        List<ShoppingListResponse> responses = shoppingListService.getArchivedLists(currentUser.id());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getShoppingListById(@PathVariable Long id) {
        ShoppingListResponse response = shoppingListService.getShoppingListById(currentUser.id(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ShoppingListResponse> restoreArchivedShoppingList(@PathVariable Long id) {
        ShoppingListResponse response = shoppingListService.restoreArchivedList(currentUser.id(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/items", "/active/items"})
    public ResponseEntity<ShoppingListResponse> createCustomItem(@Valid @RequestBody CreateCustomItemRequest request) {
        ShoppingListResponse response = shoppingListService.createCustomItem(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/items/{clientUuid}", "/active/items/{clientUuid}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteShoppingListItem(@PathVariable UUID clientUuid) {
        shoppingListService.deleteItem(currentUser.id(), clientUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/import/recipe", "/active/import/recipe"})
    public ResponseEntity<ShoppingListResponse> importRecipeIngredients(
            @Valid @RequestBody ImportRecipeRequest request) {
        ShoppingListResponse response = shoppingListService.importRecipeIngredients(currentUser.id(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/items/{clientUuid}/provenance", "/active/items/{clientUuid}/provenance"})
    public ResponseEntity<List<ItemProvenanceDto>> getItemProvenance(@PathVariable UUID clientUuid) {
        List<ItemProvenanceDto> provenance = shoppingListService.getItemProvenance(currentUser.id(), clientUuid);
        return ResponseEntity.ok(provenance);
    }
}
