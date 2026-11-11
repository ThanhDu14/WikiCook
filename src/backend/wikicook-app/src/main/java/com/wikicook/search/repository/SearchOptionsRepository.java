package com.wikicook.search.repository;

import com.wikicook.search.domain.TaxonomyRef;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Active admin-managed lists used as filters (020 FR-009, research R11); ordered by position, then name. */
@Repository
public class SearchOptionsRepository {

    private final JdbcTemplate jdbc;

    public SearchOptionsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TaxonomyRef> activeCuisines() {
        return list("SELECT id, slug, name FROM cuisines WHERE is_active ORDER BY position, name, id");
    }

    public List<TaxonomyRef> activeCategories() {
        return list("SELECT id, slug, name FROM categories WHERE is_active ORDER BY position, name, id");
    }

    public List<TaxonomyRef> activeMethods() {
        return list("SELECT id, slug, name FROM tags WHERE kind = 'METHOD' AND is_active ORDER BY position, name, id");
    }

    private List<TaxonomyRef> list(String sql) {
        return jdbc.query(sql, (rs, i) -> new TaxonomyRef(rs.getLong("id"), rs.getString("slug"), rs.getString("name")));
    }
}
