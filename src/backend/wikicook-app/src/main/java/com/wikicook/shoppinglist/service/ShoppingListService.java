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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShoppingListService {

    private static final int MAX_LIST_CAPACITY = 200;

    private static final Map<Long, AisleMeta> AISLE_METADATA = Map.of(
        1L, new AisleMeta("PRODUCE", "Rau củ & Trái cây", 1),
        2L, new AisleMeta("MEAT_SEAFOOD", "Thịt & Thủy hải sản", 2),
        3L, new AisleMeta("DAIRY_EGGS", "Trứng & Sữa", 3),
        4L, new AisleMeta("PANTRY_SPICES", "Gia vị & Đồ khô", 4),
        5L, new AisleMeta("FROZEN", "Đồ đông lạnh", 5),
        6L, new AisleMeta("GENERAL", "Đồ dùng khác", 6)
    );

    private record AisleMeta(String code, String name, int order) {}

    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final UserAislePreferenceRepository userAislePreferenceRepository;
    private final RecipeDetailPort recipeDetailPort;
    private final IngredientAggregationService ingredientAggregationService;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               ShoppingListItemRepository shoppingListItemRepository,
                               UserAislePreferenceRepository userAislePreferenceRepository,
                               RecipeDetailPort recipeDetailPort,
                               IngredientAggregationService ingredientAggregationService) {
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.userAislePreferenceRepository = userAislePreferenceRepository;
        this.recipeDetailPort = recipeDetailPort;
        this.ingredientAggregationService = ingredientAggregationService;
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse getActiveShoppingList(Long userId) {
        ShoppingList activeList = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE)
                .orElseGet(() -> createDefaultActiveList(userId));
        return toShoppingListResponse(activeList);
    }

    public ShoppingListResponse renameActiveShoppingList(Long userId, String title) {
        ShoppingList activeList = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE)
                .orElseGet(() -> createDefaultActiveList(userId));
        if (title != null && !title.isBlank()) {
            activeList.setTitle(title.trim());
            activeList = shoppingListRepository.save(activeList);
        }
        return toShoppingListResponse(activeList);
    }

    public ShoppingListResponse createCustomItem(Long userId, CreateCustomItemRequest request) {
        ShoppingList activeList = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE)
                .orElseGet(() -> createDefaultActiveList(userId));

        long currentCount = shoppingListItemRepository.countByShoppingListId(activeList.getId());
        if (currentCount == 0 && activeList.getItems() != null) {
            currentCount = activeList.getItems().size();
        }
        if (currentCount >= MAX_LIST_CAPACITY) {
            throw new CapacityExceededException("Shopping list capacity limit of " + MAX_LIST_CAPACITY + " items reached");
        }

        Long aisleId = request.aisleCategoryId() != null ? request.aisleCategoryId() : 6L;
        ShoppingListItem item = new ShoppingListItem(
                activeList,
                request.clientUuid() != null ? request.clientUuid() : UUID.randomUUID(),
                null,
                request.customName().trim(),
                request.quantity(),
                null,
                request.unitName(),
                aisleId
        );

        activeList.addItem(item);
        ShoppingList saved = shoppingListRepository.save(activeList);
        return toShoppingListResponse(saved);
    }

    public void deleteItem(Long userId, UUID clientUuid) {
        ShoppingListItem item = shoppingListItemRepository.findByClientUuid(clientUuid)
                .orElseThrow(() -> new ShoppingListNotFoundException("Shopping list item not found with clientUuid: " + clientUuid));

        if (!item.getShoppingList().getUserId().equals(userId)) {
            throw new ShoppingListNotFoundException("Item does not belong to user active list");
        }

        ShoppingList list = item.getShoppingList();
        list.removeItem(item);
        shoppingListItemRepository.delete(item);
    }

    public ShoppingListResponse archiveActiveShoppingList(Long userId) {
        Optional<ShoppingList> activeOpt = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE);
        if (activeOpt.isPresent()) {
            ShoppingList activeList = activeOpt.get();
            activeList.setStatus(ShoppingListStatus.ARCHIVED);
            activeList.setArchivedAt(Instant.now());
            shoppingListRepository.save(activeList);
        }

        ShoppingList newActive = createDefaultActiveList(userId);
        return toShoppingListResponse(newActive);
    }

    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getArchivedLists(Long userId) {
        return shoppingListRepository.findByUserIdAndStatusOrderByArchivedAtDesc(userId, ShoppingListStatus.ARCHIVED)
                .stream()
                .map(this::toShoppingListResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingListById(Long userId, Long listId) {
        ShoppingList list = shoppingListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ShoppingListNotFoundException("Shopping list not found with ID: " + listId));
        return toShoppingListResponse(list);
    }

    public ShoppingListResponse restoreArchivedList(Long userId, Long listId) {
        ShoppingList archived = shoppingListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ShoppingListNotFoundException("Shopping list not found with ID: " + listId));

        Optional<ShoppingList> currentActiveOpt = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE);
        if (currentActiveOpt.isPresent()) {
            ShoppingList currentActive = currentActiveOpt.get();
            if (!currentActive.getId().equals(listId)) {
                currentActive.setStatus(ShoppingListStatus.ARCHIVED);
                currentActive.setArchivedAt(Instant.now());
                shoppingListRepository.save(currentActive);
            }
        }

        archived.setStatus(ShoppingListStatus.ACTIVE);
        archived.setArchivedAt(null);

        if (archived.getItems() != null) {
            for (ShoppingListItem item : archived.getItems()) {
                item.setChecked(false);
                item.setCleared(false);
                item.setClearedAt(null);
            }
        }

        ShoppingList saved = shoppingListRepository.save(archived);
        return toShoppingListResponse(saved);
    }

    public ShoppingListResponse importRecipeIngredients(Long userId, ImportRecipeRequest request) {
        ShoppingList activeList = shoppingListRepository.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE)
                .orElseGet(() -> createDefaultActiveList(userId));

        if (recipeDetailPort == null) {
            throw new IllegalStateException("RecipeDetailPort is not available");
        }

        RecipeDetailPort.RecipeImportDto recipe = recipeDetailPort.getRecipeForImport(request.recipeId())
                .orElseThrow(() -> new ShoppingListNotFoundException("Recipe not found with ID: " + request.recipeId()));

        Set<Long> excluded = request.excludedIngredientIds() != null ?
                new HashSet<>(request.excludedIngredientIds()) : Collections.emptySet();

        ingredientAggregationService.aggregateRecipe(
                activeList,
                recipe,
                request.targetServings(),
                excluded
        );

        ShoppingList saved = shoppingListRepository.save(activeList);
        return toShoppingListResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemProvenanceDto> getItemProvenance(Long userId, UUID clientUuid) {
        ShoppingListItem item = shoppingListItemRepository.findByClientUuid(clientUuid)
                .orElseThrow(() -> new ShoppingListNotFoundException("Shopping list item not found with clientUuid: " + clientUuid));

        if (!item.getShoppingList().getUserId().equals(userId)) {
            throw new ShoppingListNotFoundException("Item does not belong to user");
        }

        if (item.getSources() == null) {
            return Collections.emptyList();
        }

        return item.getSources().stream()
                .map(s -> new ItemProvenanceDto(
                        s.getId(),
                        s.getRecipeId(),
                        s.getMealPlanItemId(),
                        s.getRecipeTitleSnapshot(),
                        s.getContributedQuantity(),
                        s.getUnitName()
                ))
                .collect(Collectors.toList());
    }

    private ShoppingList createDefaultActiveList(Long userId) {
        String dateSuffix = DateTimeFormatter.ofPattern("dd/MM").format(LocalDate.now());
        ShoppingList list = new ShoppingList(userId, "Danh sách mua sắm " + dateSuffix);
        return shoppingListRepository.save(list);
    }

    private ShoppingListResponse toShoppingListResponse(ShoppingList list) {
        if (list == null) {
            return null;
        }
        List<ShoppingListItem> items = list.getItems() != null ? list.getItems() : Collections.emptyList();
        int totalItems = (int) items.stream().filter(i -> !i.isCleared()).count();
        int checkedItems = (int) items.stream().filter(i -> i.isChecked() && !i.isCleared()).count();
        int clearedItems = (int) items.stream().filter(ShoppingListItem::isCleared).count();

        Map<Long, List<ShoppingListItem>> itemsByAisle = items.stream()
                .filter(i -> !i.isCleared())
                .collect(Collectors.groupingBy(ShoppingListItem::getAisleCategoryId));

        List<AisleGroupDto> aisleGroups = new ArrayList<>();
        AISLE_METADATA.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().order()))
                .forEach(entry -> {
                    Long aisleId = entry.getKey();
                    AisleMeta meta = entry.getValue();
                    List<ShoppingListItem> aisleItems = itemsByAisle.getOrDefault(aisleId, Collections.emptyList());
                    if (!aisleItems.isEmpty()) {
                        List<ShoppingListItemDto> itemDtos = aisleItems.stream()
                                .map(this::toItemDto)
                                .collect(Collectors.toList());
                        aisleGroups.add(new AisleGroupDto(aisleId, meta.code(), meta.name(), meta.order(), itemDtos));
                    }
                });

        return new ShoppingListResponse(
                list.getId(),
                list.getTitle(),
                list.getStatus() != null ? list.getStatus().name() : "ACTIVE",
                totalItems,
                checkedItems,
                clearedItems,
                aisleGroups
        );
    }

    private ShoppingListItemDto toItemDto(ShoppingListItem item) {
        String displayName = item.getCustomName() != null ? item.getCustomName() : "Nguyên liệu #" + item.getIngredientId();
        List<ItemProvenanceDto> provenanceDtos = Collections.emptyList();
        if (item.getSources() != null && !item.getSources().isEmpty()) {
            provenanceDtos = item.getSources().stream()
                    .map(s -> new ItemProvenanceDto(
                            s.getId(),
                            s.getRecipeId(),
                            s.getMealPlanItemId(),
                            s.getRecipeTitleSnapshot(),
                            s.getContributedQuantity(),
                            s.getUnitName()
                    ))
                    .collect(Collectors.toList());
        }

        return new ShoppingListItemDto(
                item.getClientUuid(),
                item.getIngredientId(),
                displayName,
                item.getQuantity(),
                item.getUnitName(),
                item.getQuantity(),
                item.getUnitName(),
                item.getAisleCategoryId(),
                item.isChecked(),
                item.isCleared(),
                item.isManuallyEdited(),
                provenanceDtos.size(),
                provenanceDtos
        );
    }
}
