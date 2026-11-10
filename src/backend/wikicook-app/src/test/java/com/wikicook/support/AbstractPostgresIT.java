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

    /**
     * Each cached Spring context keeps its own connection pool open until the JVM exits; the default 100 connections
     * run out once the suite has about ten contexts.
     */
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
    }
}
