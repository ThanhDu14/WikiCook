-- Search document of a recipe (spec 020 research R1, R2; draft-recipes-ingredients.md 4.2).
-- No trigger: whoever moves a recipe into PUBLISHED, or replaces its published content, runs
--   UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = :id
-- in the same transaction (020 contracts/integration.md 1).
-- Weight A: title. B: ingredient names and TAG / METHOD tag names (not EQUIPMENT). C: description.

CREATE OR REPLACE FUNCTION f_recipe_search_vector(p_recipe_id BIGINT)
    RETURNS tsvector
    LANGUAGE sql
    STABLE PARALLEL SAFE
AS $$
SELECT setweight(to_tsvector('simple', f_unaccent(lower(coalesce(r.title, '')))), 'A')
    || setweight(to_tsvector('simple', f_unaccent(lower(
           coalesce((SELECT string_agg(i.name, ' ')
                       FROM recipe_ingredients ri
                       JOIN ingredients i ON i.id = ri.ingredient_id
                      WHERE ri.recipe_id = r.id), '')
           || ' ' ||
           coalesce((SELECT string_agg(t.name, ' ')
                       FROM recipe_tags rt
                       JOIN tags t ON t.id = rt.tag_id
                      WHERE rt.recipe_id = r.id AND t.kind IN ('TAG', 'METHOD')), '')))), 'B')
    || setweight(to_tsvector('simple', f_unaccent(lower(coalesce(r.description, '')))), 'C')
FROM recipes r
WHERE r.id = p_recipe_id
$$;

UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE status = 'PUBLISHED';
