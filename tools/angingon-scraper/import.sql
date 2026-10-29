-- One-off import of data/recipes.jsonl (full-schema.md section 5). Run through import.sh.
-- Re-runnable: lookups are upserted and recipes whose source_url already exists are skipped.
\set ON_ERROR_STOP on
BEGIN;

-- ---------------------------------------------------------------- staging
CREATE TEMP TABLE stg_raw (doc jsonb) ON COMMIT DROP;
-- JSONL is one JSON value per line; \x01/\x02 never occur in it, so COPY leaves each line intact.
\copy stg_raw FROM '/tmp/wikicook-import/recipes.jsonl' WITH (FORMAT csv, QUOTE E'\x01', DELIMITER E'\x02')

CREATE TEMP TABLE map_units (raw_unit text PRIMARY KEY, unit_code text NOT NULL) ON COMMIT DROP;
\copy map_units FROM '/tmp/wikicook-import/units.csv' WITH (FORMAT csv, HEADER true)

CREATE TEMP TABLE map_categories (
    category_slug text PRIMARY KEY, name text NOT NULL, position smallint NOT NULL,
    for_breakfast boolean NOT NULL, for_lunch boolean NOT NULL, for_dinner boolean NOT NULL,
    is_snack boolean NOT NULL, skip boolean NOT NULL
) ON COMMIT DROP;
\copy map_categories FROM '/tmp/wikicook-import/categories.csv' WITH (FORMAT csv, HEADER true)

-- ---------------------------------------------------------------- lookups
INSERT INTO aisle_categories (code, name_vi, position) VALUES
    ('PRODUCE', 'Rau củ quả', 1), ('MEAT_SEAFOOD', 'Thịt, hải sản', 2), ('DAIRY_EGGS', 'Sữa, trứng', 3),
    ('PANTRY_SPICES', 'Gia vị, đồ khô', 4), ('FROZEN', 'Đồ đông lạnh', 5), ('GENERAL', 'Khác', 6)
ON CONFLICT (code) DO NOTHING;

INSERT INTO units (code, name_vi, unit_type, requires_quantity) VALUES
    ('g', 'g', 'MASS', true), ('kg', 'kg', 'MASS', true),
    ('ml', 'ml', 'VOLUME', true), ('l', 'lít', 'VOLUME', true),
    ('tsp', 'muỗng cà phê', 'VOLUME', true), ('tbsp', 'muỗng canh', 'VOLUME', true),
    ('muong', 'muỗng', 'VOLUME', true), ('chen', 'chén', 'VOLUME', true),
    ('to', 'tô', 'VOLUME', true), ('ly', 'ly', 'VOLUME', true),
    ('trai', 'quả', 'COUNT', true), ('cu', 'củ', 'COUNT', true), ('nhanh', 'nhánh', 'COUNT', true),
    ('tep', 'tép', 'COUNT', true), ('cay', 'cây', 'COUNT', true), ('con', 'con', 'COUNT', true),
    ('cai', 'cái', 'COUNT', true), ('goi', 'gói', 'COUNT', true), ('hop', 'hộp', 'COUNT', true),
    ('mieng', 'miếng', 'COUNT', true), ('lat', 'lát', 'COUNT', true), ('la', 'lá', 'COUNT', true),
    ('bo', 'bó', 'COUNT', true), ('ong', 'ống', 'COUNT', true), ('hat', 'hạt', 'COUNT', true),
    ('to_taste', 'vừa đủ', 'NONE', false)
ON CONFLICT (code) DO NOTHING;

INSERT INTO cuisines (name, slug, position) VALUES ('Việt Nam', 'viet-nam', 1)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, slug, position)
SELECT name, category_slug, position FROM map_categories WHERE NOT skip
ON CONFLICT (slug) DO NOTHING;

INSERT INTO users (email, display_name, role, is_system, email_verified_at)
VALUES ('system@wikicook.local', 'WikiCook', 'ADMINISTRATOR', true, now())
ON CONFLICT (email) DO NOTHING;

-- Fail loudly instead of silently dropping rows when the data has a unit/category the mappings miss.
DO $$
DECLARE missing text;
BEGIN
    SELECT string_agg(DISTINCT i->>'unit', ', ') INTO missing
    FROM stg_raw, jsonb_array_elements(doc->'ingredients') i
    WHERE i->>'unit' IS NOT NULL AND NOT EXISTS (SELECT 1 FROM map_units m WHERE m.raw_unit = i->>'unit');
    IF missing IS NOT NULL THEN RAISE EXCEPTION 'units.csv is missing: %', missing; END IF;

    SELECT string_agg(DISTINCT doc->>'category_slug', ', ') INTO missing
    FROM stg_raw WHERE NOT EXISTS (SELECT 1 FROM map_categories m WHERE m.category_slug = doc->>'category_slug');
    IF missing IS NOT NULL THEN RAISE EXCEPTION 'categories.csv is missing: %', missing; END IF;
END $$;

-- ---------------------------------------------------------------- ingredients
-- One row per normalized name, displayed with its most frequent spelling.
-- kind is OTHER until mappings/ingredients.csv exists; diet and allergen filters must not trust it yet.
INSERT INTO ingredients (name, name_normalized, kind, status)
SELECT DISTINCT ON (norm) name, norm, 'OTHER', 'APPROVED'
FROM (
    SELECT left(trim(i->>'name'), 100) AS name, f_unaccent(lower(left(trim(i->>'name'), 100))) AS norm, count(*) AS n
    FROM stg_raw, jsonb_array_elements(doc->'ingredients') i
    WHERE trim(i->>'name') <> ''
    GROUP BY 1, 2
) t
WHERE NOT EXISTS (SELECT 1 FROM ingredients g WHERE g.name_normalized = t.norm AND g.status = 'APPROVED')
ORDER BY norm, n DESC, name;

-- ---------------------------------------------------------------- recipes
CREATE TEMP TABLE new_recipes (id bigint PRIMARY KEY, doc jsonb NOT NULL) ON COMMIT DROP;

WITH src AS (
    SELECT s.doc, c.*,
           -- Some pages put the yield ("500 gr tôm") in the servings field; those cannot be PUBLISHED.
           CASE WHEN (s.doc->>'servings')::int BETWEEN 1 AND 50 THEN (s.doc->>'servings')::smallint END AS servings
    FROM stg_raw s JOIN map_categories c ON c.category_slug = s.doc->>'category_slug'
    WHERE NOT c.skip
      AND NOT EXISTS (SELECT 1 FROM recipes r WHERE r.source_url = s.doc->>'source_url')
), ins AS (
    INSERT INTO recipes (author_id, title, description, cuisine_id, category_id, difficulty,
                         prep_time_minutes, cook_time_minutes, servings,
                         for_breakfast, for_lunch, for_dinner, is_snack, video_url, video_platform,
                         status, last_wizard_step, source_name, source_url, imported_at, published_at)
    SELECT (SELECT id FROM users WHERE email = 'system@wikicook.local'),
           left(doc->>'title', 100), left(doc->>'description', 500),
           (SELECT id FROM cuisines WHERE slug = 'viet-nam'),
           (SELECT id FROM categories WHERE slug = src.category_slug),
           CASE doc->>'difficulty' WHEN 'Dễ' THEN 'EASY' WHEN 'Trung bình' THEN 'MEDIUM' WHEN 'Khó' THEN 'HARD' END,
           (doc->>'prep_time_minutes')::smallint, (doc->>'cook_time_minutes')::smallint, servings,
           for_breakfast, for_lunch, for_dinner, is_snack,
           nullif(doc->>'video_url', ''),
           CASE WHEN doc->>'video_url' ~* 'youtu' THEN 'YOUTUBE' WHEN doc->>'video_url' ~* 'tiktok' THEN 'TIKTOK' END,
           CASE WHEN servings IS NULL THEN 'DRAFT' ELSE 'PUBLISHED' END, 6,
           doc->>'source', doc->>'source_url', now(),
           CASE WHEN servings IS NULL THEN NULL ELSE now() END
    FROM src
    RETURNING id, source_url
)
INSERT INTO new_recipes SELECT ins.id, s.doc FROM ins JOIN stg_raw s ON s.doc->>'source_url' = ins.source_url;

INSERT INTO recipe_ingredients (recipe_id, group_name, ingredient_id, quantity, unit_id, amount_text, note, position)
SELECT r.id, left(i->>'group', 60), g.id,
       nullif((i->>'quantity')::numeric, 0), u.id,
       left(i->>'amount_text', 50), left(i->>'note', 100), ord
FROM new_recipes r
CROSS JOIN LATERAL jsonb_array_elements(r.doc->'ingredients') WITH ORDINALITY AS x(i, ord)
JOIN ingredients g ON g.status = 'APPROVED' AND g.name_normalized = f_unaccent(lower(left(trim(i->>'name'), 100)))
JOIN units u ON u.code = COALESCE((SELECT unit_code FROM map_units WHERE raw_unit = i->>'unit'), 'to_taste')
WHERE trim(i->>'name') <> '';

INSERT INTO recipe_steps (recipe_id, position, title, content)
SELECT r.id, ord, left(nullif(s->>'title', ''), 100), left(nullif(s->>'text', ''), 3000)
FROM new_recipes r
CROSS JOIN LATERAL jsonb_array_elements(r.doc->'steps') WITH ORDINALITY AS x(s, ord);

INSERT INTO recipe_media (recipe_id, role, position, external_url)
SELECT id, 'COVER', 0, doc->>'image_url' FROM new_recipes WHERE nullif(doc->>'image_url', '') IS NOT NULL;

INSERT INTO recipe_media (recipe_id, step_id, role, position, external_url)
SELECT r.id, st.id, 'STEP', img_ord, img
FROM new_recipes r
CROSS JOIN LATERAL jsonb_array_elements(r.doc->'steps') WITH ORDINALITY AS x(s, ord)
JOIN recipe_steps st ON st.recipe_id = r.id AND st.position = ord
CROSS JOIN LATERAL jsonb_array_elements_text(s->'images') WITH ORDINALITY AS y(img, img_ord);

SELECT (SELECT count(*) FROM stg_raw) AS rows_in_file,
       (SELECT count(*) FROM new_recipes) AS recipes_imported,
       (SELECT count(*) FROM recipes r JOIN new_recipes n USING (id) WHERE r.status = 'DRAFT') AS imported_as_draft;

COMMIT;
