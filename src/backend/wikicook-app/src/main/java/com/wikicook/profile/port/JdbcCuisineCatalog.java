package com.wikicook.profile.port;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Stand-in for 100's {@link CuisineCatalogPort}: reads {@code cuisines} directly. */
public class JdbcCuisineCatalog implements CuisineCatalogPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCuisineCatalog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CuisineName> listActive() {
        return jdbc.query("SELECT id, name FROM cuisines WHERE is_active ORDER BY position, name, id", Map.of(),
                (rs, rowNum) -> new CuisineName(rs.getLong("id"), rs.getString("name")));
    }

    @Override
    public Set<Long> activeAmong(Collection<Long> cuisineIds) {
        if (cuisineIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT id FROM cuisines WHERE id IN (:ids) AND is_active",
                Map.of("ids", cuisineIds), Long.class));
    }

    @Override
    public Map<Long, String> namesOf(Collection<Long> cuisineIds) {
        Map<Long, String> names = new HashMap<>();
        if (!cuisineIds.isEmpty()) {
            jdbc.query("SELECT id, name FROM cuisines WHERE id IN (:ids)", Map.of("ids", cuisineIds),
                    rs -> {
                        names.put(rs.getLong("id"), rs.getString("name"));
                    });
        }
        return names;
    }
}
