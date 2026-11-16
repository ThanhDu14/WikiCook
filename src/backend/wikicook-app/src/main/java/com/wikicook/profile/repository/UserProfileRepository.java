package com.wikicook.profile.repository;

import com.wikicook.profile.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The columns of {@code users} that spec 011 owns. The account projection (level 2 of research R3) and the public
 * projection (level 1) are added by the services that need them (T063, T067).
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
