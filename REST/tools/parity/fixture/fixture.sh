#!/usr/bin/env bash
# Builds the hand-checked parity database from scratch: every migration in prism/SQL except the demo data
# (002–004), then fixture-data.sql. Re-run any time; it drops and recreates only the fixture database.
#   PRISM_PSQL_BASE  psql command without -d (default: psql -h localhost -p 5432 -U postgres)
#   FIXTURE_DB       database name (default: prism_fixture; must contain "fixture")
#   PGPASSWORD       as for any psql call
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
sql_dir="$(cd "$here/../../../../SQL" && pwd)"
find_psql() {
  # PATH first, then the copies bundled with Postgres.app and pgAdmin (macOS), which aren't on the PATH by default.
  for candidate in "$(command -v psql || true)" \
                   /Applications/Postgres.app/Contents/Versions/latest/bin/psql \
                   "/Applications/pgAdmin 4.app/Contents/SharedSupport/psql"; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return; fi
  done
  echo "psql not found: add it to PATH or set PRISM_PSQL_BASE (see tools/parity/README.md)" >&2
  exit 1
}
if [ -n "${PRISM_PSQL_BASE:-}" ]; then
  read -r -a PSQL_BASE <<< "$PRISM_PSQL_BASE"
else
  PSQL_BASE=("$(find_psql)" -h localhost -p 5432 -U postgres)
fi
DB=${FIXTURE_DB:-prism_fixture}
case "$DB" in *fixture*) ;; *) echo "FIXTURE_DB must contain 'fixture' (refusing to drop '$DB')" >&2; exit 1;; esac
export PGOPTIONS='-c client_min_messages=warning'   # hide NOTICEs; warnings and errors still show

run() { "${PSQL_BASE[@]}" -d "$DB" -q -v ON_ERROR_STOP=1 "$@" >/dev/null; }
"${PSQL_BASE[@]}" -d postgres -q -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"$DB\"" -c "CREATE DATABASE \"$DB\"" >/dev/null
for script in "$sql_dir"/[0-9][0-9][0-9]-*.sql; do
  number=$(basename "$script" | cut -c1-3)
  case "$number" in 002|003|004) continue;; esac          # demo data: the fixture has its own rows
  if [ "$number" = 009 ]; then
    # Focus areas were seeded by hand in the original database; 009 needs them for its foreign key.
    run -c "INSERT INTO \"prism\".\"Focus_Areas\" (focus_area_pk, name, column_name) VALUES (1, 'Time', ''), (2, 'Organization', '')"
  fi
  run -f "$script"
done
run -f "$here/fixture-data.sql"
echo "fixture database '$DB' ready: $("${PSQL_BASE[@]}" -d "$DB" -Atc 'SELECT count(*) FROM "prism"."Sprint_Metrics_Fact"') fact rows"
