package com.wikicook.shoppinglist.repository;

import com.wikicook.shoppinglist.domain.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    Optional<ShoppingListItem> findByClientUuid(UUID clientUuid);

    Optional<ShoppingListItem> findByShoppingListIdAndIngredientIdAndIsClearedFalse(Long listId, Long ingredientId);

    long countByShoppingListIdAndIsClearedFalse(Long listId);

    long countByShoppingListId(Long listId);
}
