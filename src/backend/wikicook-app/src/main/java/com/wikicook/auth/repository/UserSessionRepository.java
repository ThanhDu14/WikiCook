package com.wikicook.auth.repository;

import com.wikicook.auth.domain.UserSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long>, UserSessionRepositoryCustom {

    /** {@code SELECT … FOR UPDATE}: two parallel refreshes with one cookie queue up instead of both rotating (R2). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.tokenHash = :hash")
    Optional<UserSession> lockByTokenHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.previousTokenHash = :hash")
    Optional<UserSession> lockByPreviousTokenHash(@Param("hash") String hash);

    /** Ends every open session of the user (password reset, suspension, ban; FR-016, FR-024). */
    @Modifying
    @Query("update UserSession s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAllOf(@Param("userId") long userId, @Param("now") Instant now);
}
