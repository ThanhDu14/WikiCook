package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@code users} as spec 010 sees it. Queries are added by the services that need them. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** {@code email} must already be normalized (FR-002); the column has the same CHECK. */
    Optional<UserAccount> findByEmail(String email);

    /** The account linked to this Google identity ({@code sub}), spec 010 US5. */
    Optional<UserAccount> findByGoogleSub(String googleSub);

    /** {@code SELECT … FOR UPDATE}: admin operations on one account queue up (research R10). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> lockById(@Param("id") long id);
}
