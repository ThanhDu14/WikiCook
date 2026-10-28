#!/usr/bin/env bash
# Import data/recipes.jsonl into the local Postgres started by src/backend/docker-compose.yml.
# Usage: ./import.sh [path/to/recipes.jsonl]
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
jsonl="${1:-$here/data/recipes.jsonl}"
compose=(docker compose -f "$here/../../src/backend/docker-compose.yml")

"${compose[@]}" exec -T db sh -c 'rm -rf /tmp/wikicook-import && mkdir -p /tmp/wikicook-import'
"${compose[@]}" cp "$jsonl" db:/tmp/wikicook-import/recipes.jsonl
"${compose[@]}" cp "$here/mappings/units.csv" db:/tmp/wikicook-import/units.csv
"${compose[@]}" cp "$here/mappings/categories.csv" db:/tmp/wikicook-import/categories.csv
"${compose[@]}" exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$here/import.sql"
"${compose[@]}" exec -T db rm -rf /tmp/wikicook-import
