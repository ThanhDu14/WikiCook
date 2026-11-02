package com.wikicook.shoppinglist.repository;

import com.wikicook.shoppinglist.domain.ShoppingList;
import com.wikicook.shoppinglist.domain.ShoppingListStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    Optional<ShoppingList> findByUserIdAndStatus(Long userId, ShoppingListStatus status);

    List<ShoppingList> findByUserIdAndStatusOrderByArchivedAtDesc(Long userId, ShoppingListStatus status);

    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);
}
