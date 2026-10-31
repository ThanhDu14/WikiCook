package com.wikicook.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class F4_ArchiveRestoreIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Long testUserId = 9401L;
    private final Long initialListId = 9401L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM shopping_lists WHERE user_id = ?", testUserId);
        jdbcTemplate.update("INSERT INTO users (id, email, display_name) OVERRIDING SYSTEM VALUE VALUES (?, 'f4_user@example.com', 'F4 User') ON CONFLICT (id) DO NOTHING", testUserId);
        jdbcTemplate.update("INSERT INTO shopping_lists (id, user_id, title, status) OVERRIDING SYSTEM VALUE VALUES (?, ?, 'Initial Active List', 'ACTIVE') ON CONFLICT (id) DO NOTHING", initialListId, testUserId);
    }

    @Test
    @DisplayName("F4: POST /active/archive then POST /{id}/restore when another active list exists must return 200")
    void archiveAndRestoreSucceedsWithoutUniqueConstraintViolation() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(testUserId));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // 1. Archive active list (creates new active list)
        ResponseEntity<String> archiveResponse = http.exchange(
                "/api/v1/shopping-lists/active/archive",
                HttpMethod.POST,
                request,
                String.class
        );
        assertThat(archiveResponse.getStatusCode()).as("POST /active/archive should succeed with 201 CREATED").isEqualTo(HttpStatus.CREATED);

        // 2. Restore previous list (must archive the current active list and make 9401 active)
        ResponseEntity<String> restoreResponse = http.exchange(
                "/api/v1/shopping-lists/" + initialListId + "/restore",
                HttpMethod.POST,
                request,
                String.class
        );
        assertThat(restoreResponse.getStatusCode()).as("POST /{id}/restore should succeed with 200 OK").isEqualTo(HttpStatus.OK);
    }
}
