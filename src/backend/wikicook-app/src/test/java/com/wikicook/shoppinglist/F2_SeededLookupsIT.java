package com.wikicook.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class F2_SeededLookupsIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("F2: after Flyway, lookup tables aisle_categories and units must have seed data")
    void lookupTablesAreSeededAfterFlyway() {
        Integer aisleCount = jdbcTemplate.queryForObject("SELECT count(*) FROM aisle_categories", Integer.class);
        Integer unitCount = jdbcTemplate.queryForObject("SELECT count(*) FROM units", Integer.class);

        assertThat(aisleCount).as("aisle_categories count must be at least 6 standard aisles").isNotNull().isGreaterThanOrEqualTo(6);
        assertThat(unitCount).as("units count must be greater than 0").isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("V403: identity sequences for aisle_categories and units must be advanced beyond seed IDs")
    void identitySequencesAreAdvancedBeyondSeedIds() {
        // units sequence must generate id > 7 without duplicate key error
        Long newUnitId = jdbcTemplate.queryForObject(
                "INSERT INTO units (code, name_vi, unit_type) VALUES ('box_seq_test', 'hộp test', 'COUNT') RETURNING id",
                Long.class);
        assertThat(newUnitId).as("Next generated unit id must be greater than seeded 7").isGreaterThan(7L);

        // aisle_categories sequence must be advanced > 6 (nextval yields > 6)
        Long nextAisleId = jdbcTemplate.queryForObject(
                "SELECT nextval(pg_get_serial_sequence('aisle_categories', 'id'))",
                Long.class);
        assertThat(nextAisleId).as("Next aisle category sequence id must be greater than seeded 6").isGreaterThan(6L);
    }
}
