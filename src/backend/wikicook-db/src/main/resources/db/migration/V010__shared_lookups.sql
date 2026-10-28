-- Lookup tables shared by several modules (full-schema.md 4.1, 4.3). Seed rows: R__seed_lookups.sql.

CREATE TABLE allergens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    name_vi    VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE aisle_categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE
        CHECK (code IN ('PRODUCE', 'MEAT_SEAFOOD', 'DAIRY_EGGS', 'PANTRY_SPICES', 'FROZEN', 'GENERAL')),
    name_vi    VARCHAR(50) NOT NULL,
    icon_name  VARCHAR(50),
    position   SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE units (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              VARCHAR(20) NOT NULL UNIQUE,
    name_vi           VARCHAR(30) NOT NULL,
    unit_type         VARCHAR(10) NOT NULL CHECK (unit_type IN ('MASS', 'VOLUME', 'COUNT', 'NONE')),
    requires_quantity BOOLEAN     NOT NULL DEFAULT true,
    is_active         BOOLEAN     NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cuisines (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    slug       VARCHAR(60) NOT NULL UNIQUE,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    position   SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    slug       VARCHAR(60) NOT NULL UNIQUE,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    position   SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tags, equipment and cooking methods share one table (full-schema 2.1).
CREATE TABLE tags (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind       VARCHAR(10) NOT NULL CHECK (kind IN ('TAG', 'EQUIPMENT', 'METHOD')),
    name       VARCHAR(60) NOT NULL,
    slug       VARCHAR(60) NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    position   SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (kind, slug)
);
