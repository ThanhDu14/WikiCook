package com.wikicook;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.support.AbstractPostgresIT;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Full app on the docker-compose image: context starts, Flyway applies every migration, health is UP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WikiCookApplicationIT extends AbstractPostgresIT {

    @Autowired
    Flyway flyway;

    @Autowired
    TestRestTemplate http;

    @Test
    void appliesAllMigrations() {
        var info = flyway.info();
        assertThat(info.pending()).isEmpty();
        assertThat(info.applied()).isNotEmpty().allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());
        assertThat(info.current().getVersion().getVersion()).isEqualTo("303");
    }

    @Test
    void healthIsUp() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void unknownPathReturnsProblemDetail() {
        ResponseEntity<String> response = http.getForEntity("/api/v1/does-not-exist", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
    }
}
