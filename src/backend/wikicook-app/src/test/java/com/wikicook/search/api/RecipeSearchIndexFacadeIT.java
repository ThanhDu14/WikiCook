package com.wikicook.search.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.search.support.SearchFixtures;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** What 060 and 100 call when a recipe becomes PUBLISHED (020 contracts/integration.md 1). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RecipeSearchIndexFacadeIT extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecipeSearchIndexFacade facade;

    @BeforeEach
    void setUp() {
        SearchFixtures.clear(jdbc);
    }

    @Test
    void reindexWritesTheCurrentContent() {
        long id = new SearchFixtures(jdbc).recipe("Phở bò").create();
        jdbc.update("UPDATE recipes SET title = 'Bún chả Hà Nội', search_vector = NULL WHERE id = ?", id);

        facade.reindex(id);

        assertThat(jdbc.queryForObject(
                "SELECT search_vector @@ to_tsquery('simple', 'bun & cha') FROM recipes WHERE id = ?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void unknownIdIsANoOp() {
        facade.reindex(-1);
    }
}
