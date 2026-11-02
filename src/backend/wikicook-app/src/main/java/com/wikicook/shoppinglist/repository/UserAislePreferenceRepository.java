package com.wikicook.shoppinglist.repository;

import com.wikicook.shoppinglist.domain.UserAislePreference;
import com.wikicook.shoppinglist.domain.UserAislePreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAislePreferenceRepository extends JpaRepository<UserAislePreference, UserAislePreferenceId> {

    Optional<UserAislePreference> findByUserIdAndIngredientId(Long userId, Long ingredientId);
}
