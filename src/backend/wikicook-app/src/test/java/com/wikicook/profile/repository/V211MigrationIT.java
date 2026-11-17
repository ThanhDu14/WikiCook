package com.wikicook.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.support.AbstractPostgresIT;
import com.wikicook.support.AuthTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T008: V211 (specs/011 data-model.md): the new columns, the 10-240 CHECK, the two new tables with their CHECKs and
 * CASCADE, and the 10 allergens. Pure SQL on the migrated schema.
 */
@SpringBootTest
@Import(AuthTestSupport.class)
class V211MigrationIT extends AbstractPostgresIT {

    private static final List<String> ALLERGEN_CODES = List.of("GLUTEN", "PEANUT", "TREE_NUT", "MILK", "EGG", "FISH",
            "CRUSTACEAN", "MOLLUSC", "SOY", "SESAME");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthTestSupport support;

    private Map<String, Object> column(String table, String column) {
        return jdbc.queryForMap("""
                SELECT data_type, character_maximum_length, is_nullable, column_default
                  FROM information_schema.columns WHERE table_name = ? AND column_name = ?
                """, table, column);
    }

    @Test
    @DisplayName("FR-003: users.bio is VARCHAR(300) and nullable")
    void bioColumn() {
        Map<String, Object> bio = column("users", "bio");
        assertThat(bio.get("data_type")).isEqualTo("character varying");
        assertThat(bio.get("character_maximum_length")).isEqualTo(300);
        assertThat(bio.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    @DisplayName("Edge: Two tabs - profile_version and dietary_version are INTEGER NOT NULL DEFAULT 0")
    void versionColumns() {
        for (String name : List.of("profile_version", "dietary_version")) {
            Map<String, Object> version = column("users", name);
            assertThat(version.get("data_type")).as(name).isEqualTo("integer");
            assertThat(version.get("is_nullable")).as(name).isEqualTo("NO");
            assertThat(String.valueOf(version.get("column_default"))).as(name).isEqualTo("0");
        }
        long user = support.member();
        assertThat(jdbc.queryForObject("SELECT profile_version FROM users WHERE id = ?", Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject("SELECT dietary_version FROM users WHERE id = ?", Integer.class, user)).isZero();
    }

    @Test
    @DisplayName("FR-015: max_cook_time_minutes accepts 10 and 240, rejects 9 and 241")
    void maxCookTimeRange() {
        long user = support.member();
        for (int accepted : new int[] {10, 240}) {
            assertThat(jdbc.update("UPDATE users SET max_cook_time_minutes = ? WHERE id = ?", accepted, user)).isOne();
        }
        for (int rejected : new int[] {9, 241}) {
            assertThatThrownBy(() -> jdbc.update("UPDATE users SET max_cook_time_minutes = ? WHERE id = ?", rejected, user))
                    .as("value %d", rejected).isInstanceOf(DataAccessException.class);
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass
                   AND conname IN ('chk_users_max_cook_time')""", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass
                   AND conname = 'users_max_cook_time_minutes_check'""", Integer.class)).isZero();
    }

    @Test
    @DisplayName("FR-019: dietary_notices has the columns and the kind CHECK of data-model.md")
    void dietaryNoticesTable() {
        long user = support.member();
        assertThat(column("dietary_notices", "subject_name").get("character_maximum_length")).isEqualTo(100);
        assertThat(column("dietary_notices", "subject_name").get("is_nullable")).isEqualTo("NO");
        assertThat(column("dietary_notices", "replacement_name").get("is_nullable")).isEqualTo("YES");
        assertThat(column("dietary_notices", "created_at").get("is_nullable")).isEqualTo("NO");
        for (String kind : List.of("INGREDIENT_MERGED", "INGREDIENT_REMOVED", "ALLERGEN_REMOVED")) {
            assertThat(jdbc.update("INSERT INTO dietary_notices (user_id, kind, subject_name) VALUES (?, ?, 'Ngò rí')",
                    user, kind)).isOne();
        }
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO dietary_notices (user_id, kind, subject_name) VALUES (?, 'OTHER', 'x')", user))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO dietary_notices (user_id, kind, subject_name) VALUES (?, 'ALLERGEN_REMOVED', NULL)", user))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("NFR-008: profile_write_requests accepts PROFILE_SAVE and AVATAR_UPLOAD only")
    void profileWriteRequestsTable() {
        long user = support.member();
        for (String kind : List.of("PROFILE_SAVE", "AVATAR_UPLOAD")) {
            assertThat(jdbc.update("INSERT INTO profile_write_requests (user_id, kind) VALUES (?, ?)", user, kind))
                    .isOne();
        }
        assertThatThrownBy(() -> jdbc.update("INSERT INTO profile_write_requests (user_id, kind) VALUES (?, 'LOGIN')", user))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes WHERE tablename = 'profile_write_requests'
                   AND indexdef LIKE '%(user_id, kind, created_at DESC)%'""", Integer.class)).isOne();
    }

    @Test
    @DisplayName("FR-032: DELETE users removes the rows of both new tables (CASCADE)")
    void deletingTheUserCascades() {
        long user = support.member();
        jdbc.update("INSERT INTO dietary_notices (user_id, kind, subject_name) VALUES (?, 'ALLERGEN_REMOVED', 'Sữa')", user);
        jdbc.update("INSERT INTO profile_write_requests (user_id, kind) VALUES (?, 'PROFILE_SAVE')", user);

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM dietary_notices WHERE user_id = ?", Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM profile_write_requests WHERE user_id = ?", Integer.class, user))
                .isZero();
    }

    @Test
    @DisplayName("FR-009: the ten allergen groups are seeded")
    void allergensAreSeeded() {
        List<String> codes = jdbc.queryForList("SELECT code FROM allergens", String.class);
        assertThat(codes).containsAll(ALLERGEN_CODES);
    }
}
