package com.wikicook.ai.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** V001 on the same image as docker-compose: extensions exist and work for Vietnamese text and vectors. */
class ExtensionsMigrationIT extends AbstractPostgresIT {

    @Test
    void installsExtensions() {
        assertThat(jdbc.queryForList("SELECT extname FROM pg_extension", String.class))
                .contains("unaccent", "pg_trgm", "vector");
    }

    @Test
    void unaccentHandlesVietnamese() {
        assertThat(jdbc.queryForObject("SELECT f_unaccent(lower('Đậu Hũ Chiên Sả Ớt'))", String.class))
                .isEqualTo("dau hu chien sa ot");
    }

    @Test
    void unaccentIsUsableInIndexes() {
        // Fails with "functions in index expression must be marked IMMUTABLE" if the wrapper is wrong.
        jdbc.execute("CREATE TABLE unaccent_index_check (name text)");
        jdbc.execute("CREATE INDEX ON unaccent_index_check (f_unaccent(name))");
        jdbc.execute("DROP TABLE unaccent_index_check");
    }

    @Test
    void cosineDistanceWorks() {
        Double distance = jdbc.queryForObject("SELECT '[1,0,0]'::vector <=> '[0,1,0]'::vector", Double.class);
        assertThat(distance).isEqualTo(1.0);
    }
}
