package com.wikicook.aiplanner.repository;

import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.common.diet.DietRuleSql;
import com.wikicook.mealplan.domain.Meal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Step 1 of generation (research R4): ids of published, non-snack recipes that suit each meal, fit the cook
 * time and break none of the hard rules. Everything later in the pipeline picks only from these ids.
 */
@Repository
public class EligibleRecipeQuery {

    private final NamedParameterJdbcTemplate jdbc;

    public EligibleRecipeQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param maxCookTimeMinutes upper bound on {@code total_time_minutes}, or {@code null} for none
     * @return for each requested meal, the eligible recipe ids in ascending order
     */
    public Map<Meal, List<Long>> eligibleByMeal(Set<Meal> meals, DietRestrictions restrictions,
                                                Integer maxCookTimeMinutes) {
        DietRuleSql.SqlClause rules = DietRuleSql.notViolating("r.id", restrictions);
        MapSqlParameterSource params = new MapSqlParameterSource(rules.params())
                .addValue("maxCookTime", maxCookTimeMinutes, java.sql.Types.INTEGER);
        Map<Meal, List<Long>> result = new EnumMap<>(Meal.class);
        for (Meal meal : meals) {
            result.put(meal, jdbc.queryForList("""
                    SELECT r.id FROM recipes r
                     WHERE r.status = 'PUBLISHED' AND NOT r.is_snack AND r.%s
                       AND (CAST(:maxCookTime AS INTEGER) IS NULL OR r.total_time_minutes <= :maxCookTime)
                       AND %s
                     ORDER BY r.id
                    """.formatted(mealColumn(meal), rules.sql()), params, Long.class));
        }
        return result;
    }

    static String mealColumn(Meal meal) {
        return switch (meal) {
            case BREAKFAST -> "for_breakfast";
            case LUNCH -> "for_lunch";
            case DINNER -> "for_dinner";
        };
    }
}
