package com.wikicook.shoppinglist.repository;

import com.wikicook.shoppinglist.domain.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByCode(String code);

    Optional<Unit> findByNameVi(String nameVi);
}
