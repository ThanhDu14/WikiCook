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
}
