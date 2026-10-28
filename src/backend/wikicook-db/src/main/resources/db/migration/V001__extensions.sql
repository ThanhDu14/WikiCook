-- Extensions shared by every module (full-schema.md section 7).
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;

-- unaccent() is only STABLE, so it cannot be used in indexes or generated columns.
-- This wrapper pins the dictionary, which makes it safe to declare IMMUTABLE.
CREATE OR REPLACE FUNCTION f_unaccent(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE PARALLEL SAFE STRICT
AS $$
SELECT public.unaccent('public.unaccent'::regdictionary, $1)
$$;
