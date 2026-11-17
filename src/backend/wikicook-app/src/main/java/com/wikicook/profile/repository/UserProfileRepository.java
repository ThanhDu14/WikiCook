package com.wikicook.profile.repository;

import com.wikicook.profile.domain.UserProfile;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The columns of {@code users} that spec 011 owns, plus two read-only projections that keep the levels of research R3
 * apart in SQL: {@link AccountRow} (level 2, the owner) and {@link PublicRow} (level 1, everybody). Neither projection
 * can carry the password hash: it is not selected.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** Level 2: what the owner sees about their account. {@code hasPassword} and {@code googleLinked} are only flags. */
    interface AccountRow {

        String getEmail();

        String getRole();

        String getStatus();

        boolean getEmailVerified();

        boolean getHasPassword();

        boolean getGoogleLinked();

        Instant getCreatedAt();
    }

    /** Level 1: the only columns a guest can ever be shown (FR-005). */
    interface PublicRow {

        Long getId();

        String getDisplayName();

        String getAvatarStorageKey();

        String getBio();
    }

    @Query(nativeQuery = true, value = """
            SELECT u.email AS email, u.role AS role, u.status AS status,
                   (u.email_verified_at IS NOT NULL) AS emailVerified,
                   (u.password_hash IS NOT NULL) AS hasPassword,
                   (u.google_sub IS NOT NULL) AS googleLinked,
                   u.created_at AS createdAt
              FROM users u WHERE u.id = :id
            """)
    Optional<AccountRow> findAccount(@Param("id") long id);

    /** A {@code BANNED} account is not found, exactly like an id that does not exist (OQ-011-03). */
    @Query(nativeQuery = true, value = """
            SELECT u.id AS id, u.display_name AS displayName, u.avatar_storage_key AS avatarStorageKey, u.bio AS bio
              FROM users u WHERE u.id = :id AND u.status <> 'BANNED'
            """)
    Optional<PublicRow> findPublic(@Param("id") long id);
}
