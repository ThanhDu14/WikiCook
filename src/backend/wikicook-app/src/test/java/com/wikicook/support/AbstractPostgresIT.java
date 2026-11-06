package com.wikicook.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for integration tests that need the real database (spec 030 T004): one Postgres container on the
 * docker-compose image, shared by every test class so Spring can cache the context; Flyway runs on startup.
 * Subclasses add {@code @SpringBootTest} or a slice annotation.
 */
public abstract class AbstractPostgresIT {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
    }
}
