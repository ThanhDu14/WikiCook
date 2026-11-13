package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code users} as spec 010 sees it. Queries are added by the services that need them. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** {@code email} must already be normalized (FR-002); the column has the same CHECK. */
    Optional<UserAccount> findByEmail(String email);
}
