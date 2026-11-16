package com.wikicook.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.mealplan.domain.DietType;
import com.wikicook.profile.domain.UserProfile;
import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T009 (research R4): the context starting proves {@code ddl-auto=validate} accepts the second entity on
 * {@code users}; the round trips prove a save writes only what changed, so it can not undo what 010 or the dietary
 * writer stored in other columns in the meantime.
 */
@SpringBootTest
@Import(AuthTestSupport.class)
class UserProfileMappingIT extends AbstractPostgresIT {

    @Autowired
    UserProfileRepository profiles;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    @Test
    @DisplayName("R4: every column of the entity round trips")
    void roundTrip() {
        long user = support.member();
        jdbc.update("""
                UPDATE users SET bio = 'Thích nấu ăn', diet_type = 'VEGAN', household_size = 4,
                                 max_cook_time_minutes = 45, avatar_storage_key = 'avatars/x.jpg' WHERE id = ?
                """, user);

        UserProfile profile = profiles.findById(user).orElseThrow();

        assertThat(profile.getBio()).isEqualTo("Thích nấu ăn");
        assertThat(profile.getDietType()).isEqualTo(DietType.VEGAN);
        assertThat(profile.getHouseholdSize()).isEqualTo((short) 4);
        assertThat(profile.getMaxCookTimeMinutes()).isEqualTo((short) 45);
        assertThat(profile.getAvatarStorageKey()).isEqualTo("avatars/x.jpg");
        assertThat(profile.getProfileVersion()).isZero();
        assertThat(profile.getDietaryVersion()).isZero();
    }

    @Test
    @DisplayName("R4: saving UserProfile leaves email and last_sign_in_at alone")
    void saveDoesNotTouchOtherColumns() {
        long user = support.member();
        Timestamp signedIn = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS));
        jdbc.update("UPDATE users SET last_sign_in_at = ? WHERE id = ?", signedIn, user);
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, user);

        tx.executeWithoutResult(status -> {
            UserProfile profile = profiles.findById(user).orElseThrow();
            profile.setBio("Bio mới");
            profiles.save(profile);
        });

        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, user)).isEqualTo(email);
        assertThat(jdbc.queryForObject("SELECT last_sign_in_at FROM users WHERE id = ?", Timestamp.class, user))
                .isEqualTo(signedIn);
        assertThat(jdbc.queryForObject("SELECT bio FROM users WHERE id = ?", String.class, user)).isEqualTo("Bio mới");
    }

    @Test
    @DisplayName("R4: @DynamicUpdate - a column changed behind the entity's back is not overwritten by a stale value")
    void dynamicUpdateWritesOnlyChangedColumns() {
        long user = support.member();

        tx.executeWithoutResult(status -> {
            UserProfile profile = profiles.findById(user).orElseThrow(); // household_size is null here
            jdbc.update("UPDATE users SET household_size = 7 WHERE id = ?", user); // the dietary writer, same transaction
            profile.setBio("Chỉ đổi bio");
            profiles.saveAndFlush(profile);
        });

        assertThat(jdbc.queryForObject("SELECT household_size FROM users WHERE id = ?", Short.class, user))
                .isEqualTo((short) 7);
    }

    @Test
    @DisplayName("Edge: Two tabs - profile_version is bumped by a save and checked by it")
    void profileVersionIsBumpedBySave() {
        long user = support.member();

        tx.executeWithoutResult(status -> {
            UserProfile profile = profiles.findById(user).orElseThrow();
            profile.setBio("v1");
            profiles.saveAndFlush(profile);
        });

        assertThat(jdbc.queryForObject("SELECT profile_version FROM users WHERE id = ?", Integer.class, user)).isOne();
    }
}
