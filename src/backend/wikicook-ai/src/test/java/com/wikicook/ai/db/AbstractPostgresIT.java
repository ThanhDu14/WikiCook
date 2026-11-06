package com.wikicook.ai.db;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One Postgres container (same image as docker-compose) for every *IT of this module, migrated with all
 * wikicook-db migrations. Stands in for 030's {@code AbstractPostgresIT} until wikicook-app has one.
 */
public abstract class AbstractPostgresIT {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    protected static final PGSimpleDataSource DATA_SOURCE;
    protected static final JdbcTemplate jdbc;
    protected static final NamedParameterJdbcTemplate namedJdbc;

    static {
        POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
        DATA_SOURCE = new PGSimpleDataSource();
        DATA_SOURCE.setUrl(POSTGRES.getJdbcUrl());
        DATA_SOURCE.setUser(POSTGRES.getUsername());
        DATA_SOURCE.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(DATA_SOURCE).load().migrate();
        jdbc = new JdbcTemplate(DATA_SOURCE);
        namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    protected static long insertUser(String email) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, display_name) VALUES (?, 'Test') RETURNING id", Long.class, email);
    }

    /** A DRAFT recipe: enough for foreign keys, no other column is required. */
    protected static long insertRecipe() {
        return jdbc.queryForObject("INSERT INTO recipes DEFAULT VALUES RETURNING id", Long.class);
    }
}
