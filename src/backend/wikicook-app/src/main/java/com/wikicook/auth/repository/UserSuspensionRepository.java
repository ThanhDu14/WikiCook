package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserSuspension;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    /** The sanction that is running now (at most one per user, V102 unique index). */
    @Query("select s from UserSuspension s where s.userId = :userId and s.liftedAt is null")
    Optional<UserSuspension> findRunning(@Param("userId") long userId);
}
